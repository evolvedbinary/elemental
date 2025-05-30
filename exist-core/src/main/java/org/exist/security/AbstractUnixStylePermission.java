/*
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.util.SyntaxException;

/**
 * All code in this class must be side-effect free
 * and not carry state, thus ensuring that thus class
 * can be used in a local or remote scenario
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractUnixStylePermission implements Permission {

    private final static Logger LOG = LogManager.getLogger(AbstractUnixStylePermission.class);

    static int ownerExecute = EXECUTE << 6;

    static int groupRead = READ << 3;
    static int groupWrite = WRITE << 3;
    static int groupExecute = EXECUTE << 3;

    static int otherRead = READ;
    static int otherWrite = WRITE;
    static int otherExecute = EXECUTE;

    static int noop = 0;

    /**
     * The symbolic mode is described by the following grammar:
     * <p>
     * mode         ::= clause [, clause ...]
     * clause       ::= [who ...] [action ...] action
     * action       ::= op [perm ...]
     * who          ::= a | u | g | o
     * op           ::= + | - | =
     * perm         ::= r | s | t | w | x
     */
    private void setUnixSymbolicMode(final String symbolicMode)
            throws SyntaxException, PermissionDeniedException {

        //TODO expand perm to full UNIX chmod i.e. perm ::= r | s | t | w | x | X | u | g | o

        final String[] clauses = symbolicMode.split(",");
        for (final String clause : clauses) {
            final String[] whoPerm = clause.split("[+\\-=]");

            int perm = 0;
            boolean uidgid = false;
            boolean sticky = false;
            //process the op first
            for (final char c : whoPerm[1].toCharArray()) {
                switch (c) {
                    case READ_CHAR:
                        perm |= READ;
                        break;

                    case WRITE_CHAR:
                        perm |= WRITE;
                        break;

                    case EXECUTE_CHAR:
                        perm |= EXECUTE;
                        break;

                    case SETUID_CHAR:
                        uidgid = true;
                        break;

                    case STICKY_CHAR:
                        sticky = true;
                        break;

                    default:
                        throw new SyntaxException("Unrecognised mode char '" + c + "'");
                }
            }


            final char[] whoose;
            if (!whoPerm[0].isEmpty()) {
                whoose = whoPerm[0].toCharArray();
            } else {
                whoose = new char[]{ALL_CHAR};
            }

            for (final char c : whoose) {
                switch (c) {
                    case ALL_CHAR:
                        final int newMode = (perm << 6) | (perm << 3) | perm | (sticky ? (STICKY << 9) : 0) | (uidgid ? ((SET_UID | SET_GID) << 9) : 0);
                        if (clause.indexOf('+') > -1) {
                            setMode(getMode() | newMode);
                        } else if (clause.indexOf('-') > -1) {
                            setMode(getMode() & ~newMode);
                        } else if (clause.indexOf('=') > -1) {
                            setMode(newMode);
                        }
                        break;

                    case USER_CHAR:
                        if (clause.indexOf('+') > -1) {
                            setOwnerMode(getOwnerMode() | perm);
                            if (uidgid) {
                                setSetUid(true);
                            }
                        } else if (clause.indexOf('-') > -1) {
                            setOwnerMode(getOwnerMode() & ~perm);
                            if (uidgid) {
                                setSetUid(false);
                            }
                        } else if (clause.indexOf('=') > -1) {
                            setOwnerMode(perm);
                            if (uidgid) {
                                setSetUid(true);
                            }
                        }
                        break;

                    case GROUP_CHAR:
                        if (clause.indexOf('+') > -1) {
                            setGroupMode(getGroupMode() | perm);
                            if (uidgid) {
                                setSetGid(true);
                            }
                        } else if (clause.indexOf('-') > -1) {
                            setGroupMode(getGroupMode() & ~perm);
                            if (uidgid) {
                                setSetGid(false);
                            }
                        } else if (clause.indexOf('=') > -1) {
                            setGroupMode(perm);
                            if (uidgid) {
                                setSetGid(true);
                            }
                        }
                        break;

                    case OTHER_CHAR:
                        if (clause.indexOf('+') > -1) {
                            setOtherMode(getOtherMode() | perm);
                            if (sticky) {
                                setSticky(true);
                            }
                        } else if (clause.indexOf('-') > -1) {
                            setOtherMode(getOtherMode() & ~perm);
                            if (sticky) {
                                setSticky(false);
                            }
                        } else if (clause.indexOf('=') > -1) {
                            setOtherMode(perm);
                            if (sticky) {
                                setSticky(true);
                            }
                        }
                        break;

                    default:
                        throw new SyntaxException("Unrecognised mode char '" + c + "'");
                }
            }
        }
    }

    /**
     * Set mode using a string. The string has the
     * following syntax:
     * <p>
     * [user|group|other]=[+|-][read|write|execute]
     * <p>
     * For example, to set read and write mode for the group, but
     * not for others:
     * <p>
     * group=+read,+write,other=-read,-write
     * <p>
     * The new settings are or'ed with the existing settings.
     *
     * @param existSymbolicMode The new mode
     * @throws SyntaxException Description of the Exception
     * @deprecated setUnixSymbolicMode should be used instead
     */
    @Deprecated
    private void setExistSymbolicMode(final String existSymbolicMode)
            throws SyntaxException, PermissionDeniedException {

        LOG.warn("Permission modes should not be set using this format '{}', consider using the UNIX symbolic mode instead", existSymbolicMode);

        int shift = 0;
        int mode = getMode();
        for (final String s : existSymbolicMode.toLowerCase().split("[=,]")) {
            if (s.equalsIgnoreCase(USER_STRING)) {
                shift = 6;
            } else if (s.equalsIgnoreCase(GROUP_STRING)) {
                shift = 3;
            } else if (s.equalsIgnoreCase(OTHER_STRING)) {
                shift = 0;
            } else {
                final int perm;

                if (s.endsWith(READ_STRING.toLowerCase())) {
                    perm = READ;
                } else if (s.endsWith(WRITE_STRING.toLowerCase())) {
                    perm = WRITE;
                } else if (s.endsWith(EXECUTE_STRING.toLowerCase())) {
                    perm = EXECUTE;
                } else {
                    throw new SyntaxException("Unrecognised mode char '" + s + "'");
                }

                if (s.startsWith("+")) {
                    mode |= (perm << shift);
                } else if (s.startsWith("-")) {
                    mode &= (~(perm << shift));
                } else {
                    throw new SyntaxException("Unrecognised mode char '" + s + "'");
                }
            }
        }
        setMode(mode);
    }

    /**
     * Simple symbolic mode is [rwxs-]{3}[rwxs-]{3}[rwxt-]{3}
     */
    private void setSimpleSymbolicMode(final String simpleSymbolicMode)
            throws SyntaxException, PermissionDeniedException {
        setMode(simpleSymbolicModeToInt(simpleSymbolicMode));
    }

    public static final Pattern UNIX_SYMBOLIC_MODE_PATTERN = Pattern.compile("((?:[augo]*(?:[+\\-=](?:[" + READ_CHAR + SETUID_CHAR + STICKY_CHAR + WRITE_CHAR + EXECUTE_CHAR + "])+)+),?)+");
    public static final Pattern EXIST_SYMBOLIC_MODE_PATTERN = Pattern.compile("(?:(?:" + USER_STRING + "|" + GROUP_STRING + "|" + OTHER_STRING + ")=(?:[+-](?:" + READ_STRING + "|" + WRITE_STRING + "|" + EXECUTE_STRING + "),?)+)+");
    public static final Pattern SIMPLE_SYMBOLIC_MODE_PATTERN = Pattern.compile("(?:(?:" + READ_CHAR + "|" + UNSET_CHAR + ")(?:" + WRITE_CHAR + "|" + UNSET_CHAR + ")(?:[" + EXECUTE_CHAR + SETUID_CHAR + SETUID_CHAR_NO_EXEC + "]|" + UNSET_CHAR + ")){2}(?:" + READ_CHAR + "|" + UNSET_CHAR + ")(?:" + WRITE_CHAR + "|" + UNSET_CHAR + ")(?:[" + EXECUTE_CHAR + STICKY_CHAR + STICKY_CHAR_NO_EXEC + "]|" + UNSET_CHAR + ")");

    /**
     * Note: we don't need @PermissionRequired(user = IS_DBA | IS_OWNER) here
     * because all of these methods delegate to the subclass implementation.
     *
     * @param modeStr The String representing a mode to set
     * @throws SyntaxException If the string syntax for the mode is not recognised.
     *                         The following syntaxes are supported. Simple symbolic,
     *                         Unix symbolic, eXist symbolic.
     * @throws PermissionDeniedException If you do not have permission to set the mode
     */
    @Override
    public final void setMode(final String modeStr)
            throws SyntaxException, PermissionDeniedException {
        final Matcher simpleSymbolicModeMatcher = SIMPLE_SYMBOLIC_MODE_PATTERN.matcher(modeStr);

        if (simpleSymbolicModeMatcher.matches()) {
            setSimpleSymbolicMode(modeStr);
        } else {
            final Matcher unixSymbolicModeMatcher = UNIX_SYMBOLIC_MODE_PATTERN.matcher(modeStr);
            if (unixSymbolicModeMatcher.matches()) {
                setUnixSymbolicMode(modeStr);
            } else {
                final Matcher existSymbolicModeMatcher = EXIST_SYMBOLIC_MODE_PATTERN.matcher(modeStr);
                if (existSymbolicModeMatcher.matches()) {
                    setExistSymbolicMode(modeStr);
                } else {
                    throw new SyntaxException("Unknown mode String: " + modeStr);
                }
            }
        }
    }

    public static int simpleSymbolicModeToInt(final String simpleModeStr) throws SyntaxException {
        int mode = 0;

        final char[] modeArray = simpleModeStr.toCharArray();
        for (int i = 0; i < modeArray.length; i++) {

            final char c = modeArray[i];
            final int shift = (i < 3 ? 6 : (i < 6 ? 3 : 0));

            switch (c) {
                case READ_CHAR:
                    mode |= (READ << shift);
                    break;
                case WRITE_CHAR:
                    mode |= (WRITE << shift);
                    break;
                case EXECUTE_CHAR:
                    mode |= (EXECUTE << shift);
                    break;
                case SETUID_CHAR_NO_EXEC:
                case SETUID_CHAR:
                    if (i < 3) {
                        mode |= (SET_UID << 9);
                    } else {
                        mode |= (SET_GID << 9);
                    }
                    if (c == SETUID_CHAR) {
                        mode |= (EXECUTE << shift);
                    }
                    break;
                case STICKY_CHAR_NO_EXEC:
                case STICKY_CHAR:
                    mode |= (STICKY << 9);
                    if (c == STICKY_CHAR) {
                        mode |= (EXECUTE << shift);
                    }
                    break;

                case UNSET_CHAR:
                    break;

                default:
                    throw new SyntaxException("Unrecognised mode char '" + c + "'");
            }
        }

        return mode;
    }

    public static String modeToSimpleSymbolicMode(final int mode) {
        final char[] ch = new char[]{
                (mode & (READ << 6)) == 0 ? UNSET_CHAR : READ_CHAR,
                (mode & (WRITE << 6)) == 0 ? UNSET_CHAR : WRITE_CHAR,
                (mode & (EXECUTE << 6)) == 0 ? UNSET_CHAR : EXECUTE_CHAR,

                (mode & (READ << 3)) == 0 ? UNSET_CHAR : READ_CHAR,
                (mode & (WRITE << 3)) == 0 ? UNSET_CHAR : WRITE_CHAR,
                (mode & (EXECUTE << 3)) == 0 ? UNSET_CHAR : EXECUTE_CHAR,

                (mode & READ) == 0 ? UNSET_CHAR : READ_CHAR,
                (mode & WRITE) == 0 ? UNSET_CHAR : WRITE_CHAR,
                (mode & EXECUTE) == 0 ? UNSET_CHAR : EXECUTE_CHAR
        };
        return String.valueOf(ch);
    }

    /**
     * Utility function for external use
     *
     * @param types Types to convert to a string representation
     * @return The string representation of the types
     */
    public static String typesToString(final int types) {
        final StringBuilder builder = new StringBuilder();

        if ((types >> 8) > 0) {
            if (((types >> 8) & READ) == READ) {
                builder.append(READ_CHAR);
            } else {
                builder.append(UNSET_CHAR);
            }

            if (((types >> 8) & WRITE) == WRITE) {
                builder.append(WRITE_CHAR);
            } else {
                builder.append(UNSET_CHAR);
            }

            if (((types >> 8) & EXECUTE) == EXECUTE) {
                builder.append(EXECUTE_CHAR);
            } else {
                builder.append(UNSET_CHAR);
            }
        }

        if ((types >> 4) > 0) {
            if (((types >> 4) & READ) == READ) {
                builder.append(READ_CHAR);
            } else {
                builder.append(UNSET_CHAR);
            }

            if (((types >> 4) & WRITE) == WRITE) {
                builder.append(WRITE_CHAR);
            } else {
                builder.append(UNSET_CHAR);
            }

            if (((types >> 4) & EXECUTE) == EXECUTE) {
                builder.append(EXECUTE_CHAR);
            } else {
                builder.append(UNSET_CHAR);
            }
        }

        if ((types & READ) == READ) {
            builder.append(READ_CHAR);
        } else {
            builder.append(UNSET_CHAR);
        }

        if ((types & WRITE) == WRITE) {
            builder.append(WRITE_CHAR);
        } else {
            builder.append(UNSET_CHAR);
        }

        if ((types & EXECUTE) == EXECUTE) {
            builder.append(EXECUTE_CHAR);
        } else {
            builder.append(UNSET_CHAR);
        }

        return builder.toString();
    }

    private static int setExecutableIfOtherCanReadOrWrite(final int mode) {
        final boolean canReadOrWrite = (mode & otherRead) + (mode & otherWrite) > 0;

        return canReadOrWrite ? otherExecute : noop;
    }

    private static int setExecutableIfGroupCanReadOrWrite(final int mode) {
        final boolean canReadOrWrite = (mode & groupRead) + (mode & groupWrite) > 0;

        return canReadOrWrite ? groupExecute : noop;
    }

    public static int safeSetExecutable(final int mode) {
        return mode
                | ownerExecute
                | setExecutableIfGroupCanReadOrWrite(mode)
                | setExecutableIfOtherCanReadOrWrite(mode)
                ;
    }
}