/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
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
package org.exist.xquery;

import org.exist.Namespaces;
import org.exist.dom.QName;

import javax.annotation.Nullable;
/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ErrorCodes {

    /**
     * A defined Error Code.
     */
    public interface IErrorCode {

        /**
         * Get the name of the error code.
         *
         * @return the name of the error code.
         */
        QName getErrorQName();

        /**
         * Get the description of the error code.
         *
         * @return the description of the error code.
         */
        @Nullable String getDescription();
    }

    public static class ErrorCode {
        private final QName errorQName;
        private @Nullable final String description;

        public ErrorCode(final String code, @Nullable final String description) {
            this.errorQName = new QName(code, Namespaces.EXIST_XQUERY_XPATH_ERROR_NS, Namespaces.EXIST_XQUERY_XPATH_ERROR_PREFIX);
            this.description = description;
        }

        public ErrorCode(final QName errorQName, final String description) {
            this.errorQName = errorQName;
            this.description = description;
        }

        public QName getErrorQName() {
            return errorQName;
        }

        @Override
        public String toString() {
            return "(" + errorQName.toString() + "): " + description;
        }

        public @Nullable String getDescription(){
            return description;
        }
    }

    /**
     * Get a defined Error Code by its qualified name.
     *
     * @param qname The qualified name of the Error Code.
     *
     * @return the corresponding Error Code.
     *
     * @throws IllegalArgumentException if there is no defined Error Code for the provided name.
     */
    public static ErrorCode fromQName(final QName qname) {
        if (Namespaces.W3C_XQUERY_XPATH_ERROR_NS.equals(qname.getNamespaceURI())) {
            return W3CErrorCode.valueOf(qname.getLocalPart()).errorCode;

        } else if (Namespaces.EXIST_XQUERY_XPATH_ERROR_NS.equals(qname.getNamespaceURI())) {
            return EXistErrorCode.valueOf(qname.getLocalPart()).errorCode;
        }

        throw new IllegalArgumentException("There is no error code defined for the name: " + qname);
    }

    /**
     * Error codes defined in W3C standards.
     */
    public enum W3CErrorCode implements IErrorCode {
        XPDY0002 ("It is a dynamic error if evaluation of an expression relies on some part of the dynamic context that has not been assigned a value."),
        XPST0003 ("It is a static error if an expression is not a valid instance of the grammar defined in A.1 EBNF."),
        XPTY0004 ("It is a type error if, during the static analysis phase, an expression is found to have a static type that is not appropriate for the context in which the expression occurs, or during the dynamic evaluation phase, the dynamic type of a value does not match a required type as specified by the matching rules in 2.5.4 SequenceType Matching."),
        XPST0005 ("During the analysis phase, it is a static error if the static type assigned to an expression other than the expression () or data(()) is empty-sequence()."),
        XPTY0006 ("(Not currently used.)"),
        XPTY0007 ("(Not currently used.)"),
        XPST0008 ("It is a static error if an expression refers to an element name, attribute name, schema type name, namespace prefix, or variable name that is not defined in the static context, except for an ElementName in an ElementTest or an AttributeName in an AttributeTest."),
        XPST0010 ("An implementation must raise a static error if it encounters a reference to an axis that it does not support."),
        XPST0017 ("It is a static error if the expanded QName and number of arguments in a function call do not match the name and arity of a function signature in the static context."),
        XPTY0018 ("It is a type error if the result of the last step in a path expression contains both nodes and atomic values."),
        XPTY0019 ("It is a type error if the result of a step (other than the last step) in a path expression contains an atomic value."),
        XPTY0020 ("It is a type error if, in an axis step, the context item is not a node."),
        XPDY0021 ("(Not currently used.)"),
        XPDY0050 ("It is a dynamic error if the dynamic type of the operand of a treat expression does not match the sequence type specified by the treat expression. This error might also be raised by a path expression beginning with \"/\" or \"//\" if the context node is not in a tree that is rooted at a document node. This is because a leading \"/\" or \"//\" in a path expression is an abbreviation for an initial step that includes the clause treat as document-node()."),
        XPST0051 ("It is a static error if a QName that is used as an AtomicType in a SequenceType is not defined in the in-scope schema types as an atomic type."),
        XPST0080 ("It is a static error if the target type of a cast or castable expression is xs:NOTATION or xs:anyAtomicType."),
        XPST0081 ("It is a static error if a QName used in an expression contains a namespace prefix that cannot be expanded into a namespace URI by using the statically known namespaces."),
        XPST0083 ("(Not currently used.)"),

        /* XQuery 1.0 http://www.w3.org/TR/xquery/#id-errors */
        XQST0009 ("An implementation that does not support the Schema Import Feature must raise a static error if a Prolog contains a schema import."),
        XQST0012 ("It is a static error if the set of definitions contained in all schemas imported by a Prolog do not satisfy the conditions for schema validity specified in Sections 3 and 5 of [XML Schema] Part 1--i.e., each definition must be valid, complete, and unique."),
        XQST0013 ("It is a static error if an implementation recognizes a pragma but determines that its content is invalid."),
        XQST0014 ("(Not currently used.)"),
        XQST0015 ("(Not currently used.)"),
        XQST0016 ("An implementation that does not support the Module Feature raises a static error if it encounters a module declaration or a module import."),
        XQST0022 ("It is a static error if the value of a namespace declaration attribute is not a URILiteral."),
        XQTY0023 ("(Not currently used.)"),
        XQTY0024 ("It is a type error if the content sequence in an element constructor contains an attribute node following a node that is not an attribute node."),
        XQDY0025 ("It is a dynamic error if any attribute of a constructed element does not have a name that is distinct from the names of all other attributes of the constructed element."),
        XQDY0026 ("It is a dynamic error if the result of the content expression of a computed processing instruction constructor contains the string \"?>\"."),
        XQDY0027 ("In a validate expression, it is a dynamic error if the root element information item in the PSVI resulting from validation does not have the expected validity property: valid if validation mode is strict, or either valid or notKnown if validation mode is lax."),
        XQTY0028 ("(Not currently used.)"),
        XQDY0029 ("(Not currently used.)"),
        XQTY0030 ("It is a type error if the argument of a validate expression does not evaluate to exactly one document or element node."),
        XQST0031 ("It is a static error if the version number specified in a version declaration is not supported by the implementation."),
        XQST0032 ("A static error is raised if a Prolog contains more than one base URI declaration."),
        XQST0033 ("It is a static error if a module contains multiple bindings for the same namespace prefix."),
        XQST0034 ("It is a static error if multiple functions declared or imported by a module have the number of arguments and their expanded QNames are equal (as defined by the eq operator)."),
        XQST0035 ("It is a static error to import two schema components that both define the same name in the same symbol space and in the same scope."),
        XQST0036 ("It is a static error to import a module if the importing module's in-scope schema types do not include definitions for the schema type names that appear in the declarations of variables and functions (whether in an argument type or return type) that are present in the imported module and are referenced in the importing module."),
        XQST0037 ("(Not currently used.)"),
        XQST0038 ("It is a static error if a Prolog contains more than one default collation declaration, or the value specified by a default collation declaration is not present in statically known collations."),
        XQST0039 ("It is a static error for a function declaration to have more than one parameter with the same name."),
        XQST0040 ("It is a static error if the attributes specified by a direct element constructor do not have distinct expanded QNames."),
        XQDY0041 ("It is a dynamic error if the value of the name expression in a computed processing instruction constructor cannot be cast to the type xs:NCName."),
        XQST0042 ("(Not currently used.)"),
        XQST0043 ("(Not currently used.)"),
        XQDY0044 ("It is a dynamic error if the node-name property of the node constructed by a computed attribute constructor is in the namespace http://www.w3.org/2000/xmlns/ (corresponding to namespace prefix xmlns), or is in no namespace and has local name xmlns."),
        XQST0045 ("It is a static error if the function name in a function declaration is in one of the following namespaces: http://www.w3.org/XML/1998/namespace, http://www.w3.org/2001/XMLSchema, http://www.w3.org/2001/XMLSchema-instance, http://www.w3.org/2005/xpath-functions."),
        XQST0046 ("An implementation MAY raise a static error if the value of a URILiteral is of nonzero length and is not in the lexical space of xs:anyURI."),
        XQST0047 ("It is a static error if multiple module imports in the same Prolog specify the same target namespace."),
        XQST0048 ("It is a static error if a function or variable declared in a library module is not in the target namespace of the library module."),
        XQST0049 ("It is a static error if two or more variables declared or imported by a module have equal expanded QNames (as defined by the eq operator.)"),
        XQDY0052 ("(Not currently used.)"),
        XQST0053 ("(Not currently used.)"),
        XQST0054 ("It is a static error if a variable depends on itself."),
        XQST0055 ("It is a static error if a Prolog contains more than one copy-namespaces declaration."),
        XQST0056 ("(Not currently used.)"),
        XQST0057 ("It is a static error if a schema import binds a namespace prefix but does not specify a target namespace other than a zero-length string."),
        XQST0058 ("It is a static error if multiple schema imports specify the same target namespace."),
        XQST0059 ("It is a static error if an implementation is unable to process a schema or module import by finding a schema or module with the specified target namespace."),
        XQST0060 ("It is a static error if the name of a function in a function declaration is not in a namespace (expanded QName has a null namespace URI)."),
        XQDY0061 ("It is a dynamic error if the operand of a validate expression is a document node whose children do not consist of exactly one element node and zero or more comment and processing instruction nodes, in any order."),
        XQDY0062 ("(Not currently used.)"),
        XQST0063 ("(Not currently used.)"),
        XQDY0064 ("It is a dynamic error if the value of the name expression in a computed processing instruction constructor is equal to \"XML\" (in any combination of upper and lower case)."),
        XQST0065 ("A static error is raised if a Prolog contains more than one ordering mode declaration."),
        XQST0066 ("A static error is raised if a Prolog contains more than one default element/type namespace declaration, or more than one default function namespace declaration."),
        XQST0067 ("A static error is raised if a Prolog contains more than one construction declaration."),
        XQST0068 ("A static error is raised if a Prolog contains more than one boundary-space declaration."),
        XQST0069 ("A static error is raised if a Prolog contains more than one empty order declaration."),
        XQST0070 ("A static error is raised if a namespace URI is bound to the predefined prefix xmlns, or if a namespace URI other than http://www.w3.org/XML/1998/namespace is bound to the prefix xml, or if the prefix xml is bound to a namespace URI other than http://www.w3.org/XML/1998/namespace."),
        XQST0071 ("A static error is raised if the namespace declaration attributes of a direct element constructor do not have distinct names."),
        XQDY0072 ("It is a dynamic error if the result of the content expression of a computed comment constructor contains two adjacent hyphens or ends with a hyphen."),
        XQST0073 ("It is a static error if the graph of module imports contains a cycle (that is, if there exists a sequence of modules M1 ... Mn such that each Mi imports Mi+1  and Mn imports M1), unless all the modules in the cycle share a common namespace."),
        XQDY0074 ("It is a dynamic error if the value of the name expression in a computed element or attribute constructor cannot be converted to an expanded QName (for example, because it contains a namespace prefix not found in statically known namespaces.)"),
        XQST0075 ("An implementation that does not support the Validation Feature must raise a static error if it encounters a validate expression."),
        XQST0076 ("It is a static error if a collation subclause in an order by clause of a FLWOR expression does not identify a collation that is present in statically known collations."),
        XQST0077 ("(Not currently used.)"),
        XQST0078 ("(Not currently used.)"),
        XQST0079 ("It is a static error if an extension expression contains neither a pragma that is recognized by the implementation nor an expression enclosed in curly braces."),
        XQST0082 ("(Not currently used.)"),
        XQDY0084 ("It is a dynamic error if the element validated by a validate statement does not have a top-level element declaration in the in-scope element declarations, if validation mode is strict."),
        XQST0085 ("It is a static error if the namespace URI in a namespace declaration attribute is a zero-length string, and the implementation does not support [XML Names 1.1]."),
        XQTY0086 ("It is a type error if the typed value of a copied element or attribute node is namespace-sensitive when construction mode is preserve and copy-namespaces mode is no-preserve."),
        XQST0087 ("It is a static error if the encoding specified in a Version Declaration does not conform to the definition of EncName specified in [XML 1.0]."),
        XQST0088 ("It is a static error if the literal that specifies the target namespace in a module import or a module declaration is of zero length."),
        XQST0089 ("It is a static error if a variable bound in a for clause of a FLWOR expression, and its associated positional variable, do not have distinct names (expanded QNames)."),
        XQST0090 ("It is a static error if a character reference does not identify a valid character in the version of XML that is in use."),
        XQDY0091 ("An implementation MAY raise a dynamic error if an xml:id error, as defined in [XML ID], is encountered during construction of an attribute named xml:id."),
        XQDY0092 ("An implementation MAY raise a dynamic error  if a constructed attribute named xml:space has a value other than preserve or default."),
        XQST0093 ("It is a static error to import a module M1 if there exists a sequence of modules M1 ... Mi ... M1 such that each module directly depends on the next module in the sequence (informally, if M1 depends on itself through some chain of module dependencies.)"),
        XQST0094 ("The name of each grouping variable must be equal (by the eq operator on expanded QNames) to the name of a variable in the input tuple stream."),
        XQST0098 ("It is a static error if, for any named or unnamed decimal format, the properties representing characters used in a picture string do not each have distinct values. The following properties represent characters used in a picture string: decimal-separator, exponent-separator, grouping-separator, percent, per-mille, the family of ten decimal digits starting with zero-digit, digit, and pattern-separator."),
        XQDY0101 ("An error is raised if a computed namespace constructor attempts to do any of the following: Bind the prefix xml to some namespace URI other than http://www.w3.org/XML/1998/namespace. Bind a prefix other than xml to the namespace URI http://www.w3.org/XML/1998/namespace. Bind the prefix xmlns to any namespace URI. Bind a prefix to the namespace URI http://www.w3.org/2000/xmlns/. Bind any prefix (including the empty prefix) to a zero-length namespace URI."),
        XQDY0102 ("If the name of an element in an element constructor is in no namespace, creating a default namespace for that element using a computed namespace constructor is an error."),
        XQST0103 ("All variables in a window clause must have distinct names."),
        XQST0111 ("It is a static error for a query prolog to contain two decimal formats with the same name, or to contain two default decimal formats."),
        XQDY0137 ("No two keys in a map may have the same key value"),
        XQDY0138 ("Position n does not exist in this array"),
        XUDY0023 ("It is a dynamic error if an insert, replace, or rename expression affects an element node by introducing a new namespace binding that conflicts with one of its existing namespace bindings."),

        /* XQuery 1.0 and XPath 2.0 Functions and Operators http://www.w3.org/TR/xpath-functions/#error-summary */
        FOER0000 ("Unidentified error."),
        FOAR0001 ("Division by zero."),
        FOAR0002 ("Numeric operation overflow/underflow."),
        FOCA0001 ("Input value too large for decimal."),
        FOCA0002 ("Invalid lexical value."),
        FOCA0003 ("Input value too large for integer."),
        FOCA0005 ("NaN supplied as float/double value."),
        FOCA0006 ("String to be cast to decimal has too many digits of precision."),
        FOCH0001 ("Code point not valid."),
        FOCH0002 ("Unsupported collation."),
        FOCH0003 ("Unsupported normalization form."),
        FOCH0004 ("Collation does not support collation units."),
        FODC0001 ("No context document."),
        FODC0002 ("Error retrieving resource."),
        FODC0003 ("Function stability not defined."),
        FODC0004 ("Invalid argument to fn:collection or fn:uri-collection."),
        FODC0005 ("Invalid argument to fn:doc or fn:doc-available."),
        FODT0001 ("Overflow/underflow in date/time operation."),
        FODT0002 ("Overflow/underflow in duration operation."),
        FODT0003 ("Invalid timezone value."),
        FONS0004 ("No namespace found for prefix."),
        FONS0005 ("Base-uri not defined in the static context."),
        FORG0001 ("Invalid value for cast/constructor."),
        FORG0002 ("Invalid argument to fn:resolve-uri()."),
        FORG0003 ("fn:zero-or-one called with a sequence containing more than one item."),
        FORG0004 ("fn:one-or-more called with a sequence containing no items."),
        FORG0005 ("fn:exactly-one called with a sequence containing zero or more than one item."),
        FORG0006 ("Invalid argument type."),
        FORG0008 ("Both arguments to fn:dateTime have a specified timezone."),
        FORG0009 ("Error in resolving a relative URI against a base URI in fn:resolve-uri."),
        FORG0010 ("Invalid date/time."),
        FORX0001 ("Invalid regular expression. flags"),
        FORX0002 ("Invalid regular expression."),
        FORX0003 ("Regular expression matches zero-length string."),
        FORX0004 ("Invalid replacement string."),
        FOTY0012 ("Argument node does not have a typed value."),
        FOTY0013 ("The argument to fn:data() contains a function item."),

        /* XSLT 2.0 and XQuery 1.0 Serialization http://www.w3.org/TR/xslt-xquery-serialization/#serial-err */
        SENR0001 ("It is an error if an item in S6 in sequence normalization is an attribute node or a namespace node."),
        SERE0003 ("It is an error if the serializer is unable to satisfy the rules for either a well-formed XML document entity or a well-formed XML external general parsed entity, or both, except for content modified by the character expansion phase of serialization."),
        SEPM0004 ("It is an error to specify the doctype-system parameter, or to specify the standalone parameter with a value other than omit, if the instance of the data model contains text nodes or multiple element nodes as children of the root node."),
        SERE0005 ("It is an error if the serialized result would contain an NCName Names that contains a character that is not permitted by the version of Namespaces in XML specified by the version parameter."),
        SERE0006 ("It is an error if the serialized result would contain a character that is not permitted by the version of XML specified by the version parameter."),
        SESU0007 ("It is an error if an output encoding other than UTF-8 or UTF-16 is requested and the serializer does not support that encoding."),
        SERE0008 ("It is an error if a character that cannot be represented in the encoding that the serializer is using for output appears in a context where character references are not allowed (for example if the character occurs in the name of an element)."),
        SEPM0009 ("It is an error if the omit-xml-declaration parameter has the value yes, and the standalone attribute has a value other than omit; or the version parameter has a value other than 1.0 and the doctype-system parameter is specified."),
        SEPM0010 ("It is an error if the output method is xml, the value of the undeclare-prefixes parameter is yes, and the value of the version parameter is 1.0."),
        SESU0011 ("It is an error if the value of the normalization-form parameter specifies a normalization form that is not supported by the serializer."),
        SERE0012 ("It is an error if the value of the normalization-form parameter is fully-normalized and any relevant construct of the result begins with a combining character."),
        SESU0013 ("It is an error if the serializer does not support the version of XML or HTML specified by the version parameter."),
        SERE0014 ("It is an error to use the HTML output method when characters which are legal in XML but not in HTML, specifically the control characters #x7F-#x9F, appear in the instance of the data model."),
        SERE0015 ("It is an error to use the HTML output method when > appears within a processing instruction in the data model instance being serialized."),
        SEPM0016 ("It is a an error if a parameter value is invalid for the defined domain."),
        SEPM0017 ("It is an error if evaluating an expression in order to extract the setting of a serialization parameter from a data model instance would yield an error."),
        SEPM0018 ("It is an error if evaluating an expression in order to extract the setting of the use-character-maps serialization parameter from a data model instance would yield a sequence of length greater than one."),

        /* XQuery 3.1 Serialization */
        SEPM0019 ("It is an error if an instance of the data model used to specify the settings of serialization parameters specifies the value of the same parameter more than once."),
        SERE0021("It is an error if a sequence being serialized using the JSON output method includes items for which no rules are provided in the appropriate section of the serialization rules"),

        /* XQuery 3.0 functions and operators */
        FOAP0001 ("Wrong number of arguments"),
        FODC0006 ("String passed to fn:parse-xml is not a well-formed XML document."),
        FODF1280 ("Invalid decimal format name."),
        FODF1310 ("Invalid decimal format picture string."),
        FOFD1340 ("Invalid date/time formatting picture string"),
        FOFD1350 ("Invalid date/time formatting component"),

        /* XQuery and XPath Full Text 1.0 */
        FTDY0020 ("It is a dynamic error if, when \"wildcards\" is in effect, a query string violates wildcard syntax."),

        /* XQuery 3.1 */
        XQTY0105 ("It is a type error if the content sequence in an element constructor contains a function."),
        FOAY0001 ("Array index out of bounds."),
        FOAY0002 ("Negative array length."),
        FOJS0001 ("JSON syntax error."),
        FOJS0002 ("JSON invalid character."),
        FOJS0003 ("JSON duplicate keys."),
        FOJS0005 ("Invalid options."),
        FOJS0006 ("Invalid XML representation of JSON."),
        FOJS0007 ("Bad JSON escape sequence."),
        FOUT1170 ("Invalid $href argument to fn:unparsed-text() (etc.)"),
        FOUT1190 ("Cannot decode resource retrieved by fn:unparsed-text() (etc.)"),
        FOUT1200 ("Cannot infer encoding of resource retrieved by fn:unparsed-text() (etc.)"),
        FOQM0001 ("Module URI is a zero-length string"),
        FOQM0002 ("Module URI not found."),
        FOQM0003 ("Static error in dynamically-loaded XQuery module."),
        FOQM0005 ("Parameter for dynamically-loaded XQuery " +
                      "module has incorrect type"),
        FOQM0006 ("No suitable XQuery processor available."),
        FOXT0001 ("No suitable XSLT processor available."),
        FOXT0002 ("Invalid parameters to XSLT transformation"),
        FOXT0003 ("XSLT transformation failed"),
        FOXT0004 ("XSLT transformation has been disabled"),
        FOXT0006 ("XSLT output contains non-accepted characters"),
        XTSE0165 ("It is a static error if the processor is not able to retrieve the resource identified by the URI reference [ in the href attribute of xsl:include or xsl:import] , or if the resource that is retrieved does not contain a stylesheet module conforming to this specification.");

        private final ErrorCode errorCode;

        W3CErrorCode(final String description) {
            this.errorCode = new ErrorCode(new QName(name(), Namespaces.W3C_XQUERY_XPATH_ERROR_NS, Namespaces.W3C_XQUERY_XPATH_ERROR_PREFIX), description);
        }

        @Override
        public QName getErrorQName() {
            return errorCode.getErrorQName();
        }

        @Override
        public @Nullable String getDescription() {
            return errorCode.getDescription();
        }

        /**
         * Get the error code.
         *
         * @return the error code.
         */
        public ErrorCode getErrorCode() {
            return errorCode;
        }
    }

    /**
     * eXist specific XQuery and XPath errors.
     * <p>
     * Codes have the format [EX][XQ|XP][DY|SE|ST][nnnn]
     * EX = eXist
     * XQ = XQuery
     * XP = XPath
     * DY = Dynamic
     * SE = Serialization
     * ST = Static
     * nnnn = number
     * </p>
     */
    public enum EXistErrorCode implements IErrorCode {
        EXXQDY0001 ("Index cannot be applied to the given expression."),
        EXXQDY0002 ("Error parsing XML."),
        EXXQDY0003 ("Only Supported for xquery version \"3.0\" and later."),
        EXXQDY0004 ("Only Supported for xquery version \"3.1\" and later."),
        EXXQDY0005 ("No function call details were provided when trying to execute a Library Module."),
        EXXQDY0006 ("Unable to find named function when trying to execute a Library Module."),
        EXXQST0001 ("Java binding is disabled in the current configuration."),
        EXXQST0002 ("No Java binding possible for the indicated Java class."),
        EXXQST0003 ("No Java binding possible for the indicated Java field/method."),

        EXMPDY001 ("Key should be a single, atomic value"),

        /**
         * @deprecated Uses of this should be replaced with defined error codes.
         */
        @Deprecated
        ERROR ("Error.");

        private final ErrorCode errorCode;

        EXistErrorCode(final String description) {
            this.errorCode = new ErrorCode(new QName(name(), Namespaces.EXIST_XQUERY_XPATH_ERROR_NS, Namespaces.EXIST_XQUERY_XPATH_ERROR_PREFIX), description);
        }

        @Override
        public QName getErrorQName() {
            return errorCode.getErrorQName();
        }

        @Override
        public @Nullable String getDescription() {
            return errorCode.getDescription();
        }

        /**
         * Get the error code.
         *
         * @return the error code.
         */
        public ErrorCode getErrorCode() {
            return errorCode;
        }
    }

    public static class JavaErrorCode extends ErrorCode {
        private JavaErrorCode(final QName qname, @Nullable final String description) {
            super(qname, description);
        }

        public static JavaErrorCode fromThrowable(final Throwable throwable) {
            final QName errorQName = new QName(throwable.getClass().getName(), Namespaces.EXIST_JAVA_BINDING_NS, Namespaces.EXIST_JAVA_BINDING_NS_PREFIX);
            @Nullable final String description;
            if (throwable.getMessage() != null) {
                description = throwable.getMessage();
            } else if (throwable.getCause() != null) {
                description = throwable.getCause().getMessage();
            } else {
                description = null;
            }
            return new JavaErrorCode(errorQName, description);
        }
    }

    public static class DynamicErrorCode extends ErrorCode {
        public DynamicErrorCode(final QName qname, @Nullable final String description) {
            super(qname, description);
        }
    }

    /**
     * @deprecated Use {@link W3CErrorCode#XPDY0002}.
     */
    @Deprecated
    public static final ErrorCode XPDY0002 = W3CErrorCode.XPDY0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPST0003}.
     */
    @Deprecated
    public static final ErrorCode XPST0003 = W3CErrorCode.XPST0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPTY0004}.
     */
    @Deprecated
    public static final ErrorCode XPTY0004 = W3CErrorCode.XPTY0004.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPST0005}.
     */
    @Deprecated
    public static final ErrorCode XPST0005 = W3CErrorCode.XPST0005.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPTY0006}.
     */
    @Deprecated
    public static final ErrorCode XPTY0006 = W3CErrorCode.XPTY0006.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPTY0007}.
     */
    @Deprecated
    public static final ErrorCode XPTY0007 = W3CErrorCode.XPTY0007.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPST0008}.
     */
    @Deprecated
    public static final ErrorCode XPST0008 = W3CErrorCode.XPST0008.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPST0010}.
     */
    @Deprecated
    public static final ErrorCode XPST0010 = W3CErrorCode.XPST0010.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPST0017}.
     */
    @Deprecated
    public static final ErrorCode XPST0017 = W3CErrorCode.XPST0017.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPTY0018}.
     */
    @Deprecated
    public static final ErrorCode XPTY0018 = W3CErrorCode.XPTY0018.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPTY0019}.
     */
    @Deprecated
    public static final ErrorCode XPTY0019 = W3CErrorCode.XPTY0019.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPTY0020}.
     */
    @Deprecated
    public static final ErrorCode XPTY0020 = W3CErrorCode.XPTY0020.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPDY0021}.
     */
    @Deprecated
    public static final ErrorCode XPDY0021 = W3CErrorCode.XPDY0021.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPDY0050}.
     */
    @Deprecated
    public static final ErrorCode XPDY0050 = W3CErrorCode.XPDY0050.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPST0051}.
     */
    @Deprecated
    public static final ErrorCode XPST0051 = W3CErrorCode.XPST0051.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPST0080}.
     */
    @Deprecated
    public static final ErrorCode XPST0080 = W3CErrorCode.XPST0080.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPST0081}.
     */
    @Deprecated
    public static final ErrorCode XPST0081 = W3CErrorCode.XPST0081.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XPST0083}.
     */
    @Deprecated
    public static final ErrorCode XPST0083 = W3CErrorCode.XPST0083.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0009}.
     */
    @Deprecated
    public static final ErrorCode XQST0009 = W3CErrorCode.XQST0009.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0012}.
     */
    @Deprecated
    public static final ErrorCode XQST0012 = W3CErrorCode.XQST0012.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0013}.
     */
    @Deprecated
    public static final ErrorCode XQST0013 = W3CErrorCode.XQST0013.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0014}.
     */
    @Deprecated
    public static final ErrorCode XQST0014 = W3CErrorCode.XQST0014.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0015}.
     */
    @Deprecated
    public static final ErrorCode XQST0015 = W3CErrorCode.XQST0015.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0016}.
     */
    @Deprecated
    public static final ErrorCode XQST0016 = W3CErrorCode.XQST0016.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0022}.
     */
    @Deprecated
    public static final ErrorCode XQST0022 = W3CErrorCode.XQST0022.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQTY0023}.
     */
    @Deprecated
    public static final ErrorCode XQTY0023 = W3CErrorCode.XQTY0023.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQTY0024}.
     */
    @Deprecated
    public static final ErrorCode XQTY0024 = W3CErrorCode.XQTY0024.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0025}.
     */
    @Deprecated
    public static final ErrorCode XQDY0025 = W3CErrorCode.XQDY0025.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0026}.
     */
    @Deprecated
    public static final ErrorCode XQDY0026 = W3CErrorCode.XQDY0026.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0027}.
     */
    @Deprecated
    public static final ErrorCode XQDY0027 = W3CErrorCode.XQDY0027.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQTY0028}.
     */
    @Deprecated
    public static final ErrorCode XQTY0028 = W3CErrorCode.XQTY0028.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0029}.
     */
    @Deprecated
    public static final ErrorCode XQDY0029 = W3CErrorCode.XQDY0029.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQTY0030}.
     */
    @Deprecated
    public static final ErrorCode XQTY0030 = W3CErrorCode.XQTY0030.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0031}.
     */
    @Deprecated
    public static final ErrorCode XQST0031 = W3CErrorCode.XQST0031.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0032}.
     */
    @Deprecated
    public static final ErrorCode XQST0032 = W3CErrorCode.XQST0032.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0033}.
     */
    @Deprecated
    public static final ErrorCode XQST0033 = W3CErrorCode.XQST0033.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0034}.
     */
    @Deprecated
    public static final ErrorCode XQST0034 = W3CErrorCode.XQST0034.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0035}.
     */
    @Deprecated
    public static final ErrorCode XQST0035 = W3CErrorCode.XQST0035.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0036}.
     */
    @Deprecated
    public static final ErrorCode XQST0036 = W3CErrorCode.XQST0036.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0037}.
     */
    @Deprecated
    public static final ErrorCode XQST0037 = W3CErrorCode.XQST0037.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0038}.
     */
    @Deprecated
    public static final ErrorCode XQST0038 = W3CErrorCode.XQST0038.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0039}.
     */
    @Deprecated
    public static final ErrorCode XQST0039 = W3CErrorCode.XQST0039.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0040}.
     */
    @Deprecated
    public static final ErrorCode XQST0040 = W3CErrorCode.XQST0040.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0041}.
     */
    @Deprecated
    public static final ErrorCode XQDY0041 = W3CErrorCode.XQDY0041.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0042}.
     */
    @Deprecated
    public static final ErrorCode XQST0042 = W3CErrorCode.XQST0042.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0043}.
     */
    @Deprecated
    public static final ErrorCode XQST0043 = W3CErrorCode.XQST0043.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0044}.
     */
    @Deprecated
    public static final ErrorCode XQDY0044 = W3CErrorCode.XQDY0044.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0045}.
     */
    @Deprecated
    public static final ErrorCode XQST0045 = W3CErrorCode.XQST0045.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0046}.
     */
    @Deprecated
    public static final ErrorCode XQST0046 = W3CErrorCode.XQST0046.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0047}.
     */
    @Deprecated
    public static final ErrorCode XQST0047 = W3CErrorCode.XQST0047.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0048}.
     */
    @Deprecated
    public static final ErrorCode XQST0048 = W3CErrorCode.XQST0048.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0049}.
     */
    @Deprecated
    public static final ErrorCode XQST0049 = W3CErrorCode.XQST0049.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0052}.
     */
    @Deprecated
    public static final ErrorCode XQDY0052 = W3CErrorCode.XQDY0052.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0053}.
     */
    @Deprecated
    public static final ErrorCode XQST0053 = W3CErrorCode.XQST0053.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0054}.
     */
    @Deprecated
    public static final ErrorCode XQST0054 = W3CErrorCode.XQST0054.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0055}.
     */
    @Deprecated
    public static final ErrorCode XQST0055 = W3CErrorCode.XQST0055.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0056}.
     */
    @Deprecated
    public static final ErrorCode XQST0056 = W3CErrorCode.XQST0056.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0057}.
     */
    @Deprecated
    public static final ErrorCode XQST0057 = W3CErrorCode.XQST0057.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0058}.
     */
    @Deprecated
    public static final ErrorCode XQST0058 = W3CErrorCode.XQST0058.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0059}.
     */
    @Deprecated
    public static final ErrorCode XQST0059 = W3CErrorCode.XQST0059.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0060}.
     */
    @Deprecated
    public static final ErrorCode XQST0060 = W3CErrorCode.XQST0060.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0061}.
     */
    @Deprecated
    public static final ErrorCode XQDY0061 = W3CErrorCode.XQDY0061.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0062}.
     */
    @Deprecated
    public static final ErrorCode XQDY0062 = W3CErrorCode.XQDY0062.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0063}.
     */
    @Deprecated
    public static final ErrorCode XQST0063 = W3CErrorCode.XQST0063.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0064}.
     */
    @Deprecated
    public static final ErrorCode XQDY0064 = W3CErrorCode.XQDY0064.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0065}.
     */
    @Deprecated
    public static final ErrorCode XQST0065 = W3CErrorCode.XQST0065.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0066}.
     */
    @Deprecated
    public static final ErrorCode XQST0066 = W3CErrorCode.XQST0066.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0067}.
     */
    @Deprecated
    public static final ErrorCode XQST0067 = W3CErrorCode.XQST0067.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0068}.
     */
    @Deprecated
    public static final ErrorCode XQST0068 = W3CErrorCode.XQST0068.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0069}.
     */
    @Deprecated
    public static final ErrorCode XQST0069 = W3CErrorCode.XQST0069.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0070}.
     */
    @Deprecated
    public static final ErrorCode XQST0070 = W3CErrorCode.XQST0070.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0071}.
     */
    @Deprecated
    public static final ErrorCode XQST0071 = W3CErrorCode.XQST0071.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0072}.
     */
    @Deprecated
    public static final ErrorCode XQDY0072 = W3CErrorCode.XQDY0072.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0073}.
     */
    @Deprecated
    public static final ErrorCode XQST0073 = W3CErrorCode.XQST0073.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0074}.
     */
    @Deprecated
    public static final ErrorCode XQDY0074 = W3CErrorCode.XQDY0074.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0075}.
     */
    @Deprecated
    public static final ErrorCode XQST0075 = W3CErrorCode.XQST0075.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0076}.
     */
    @Deprecated
    public static final ErrorCode XQST0076 = W3CErrorCode.XQST0076.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0077}.
     */
    @Deprecated
    public static final ErrorCode XQST0077 = W3CErrorCode.XQST0077.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0078}.
     */
    @Deprecated
    public static final ErrorCode XQST0078 = W3CErrorCode.XQST0078.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0079}.
     */
    @Deprecated
    public static final ErrorCode XQST0079 = W3CErrorCode.XQST0079.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0082}.
     */
    @Deprecated
    public static final ErrorCode XQST0082 = W3CErrorCode.XQST0082.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0084}.
     */
    @Deprecated
    public static final ErrorCode XQDY0084 = W3CErrorCode.XQDY0084.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0085}.
     */
    @Deprecated
    public static final ErrorCode XQST0085 = W3CErrorCode.XQST0085.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQTY0086}.
     */
    @Deprecated
    public static final ErrorCode XQTY0086 = W3CErrorCode.XQTY0086.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0087}.
     */
    @Deprecated
    public static final ErrorCode XQST0087 = W3CErrorCode.XQST0087.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0088}.
     */
    @Deprecated
    public static final ErrorCode XQST0088 = W3CErrorCode.XQST0088.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0089}.
     */
    @Deprecated
    public static final ErrorCode XQST0089 = W3CErrorCode.XQST0089.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0090}.
     */
    @Deprecated
    public static final ErrorCode XQST0090 = W3CErrorCode.XQST0090.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0091}.
     */
    @Deprecated
    public static final ErrorCode XQDY0091 = W3CErrorCode.XQDY0091.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0092}.
     */
    @Deprecated
    public static final ErrorCode XQDY0092 = W3CErrorCode.XQDY0092.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0093}.
     */
    @Deprecated
    public static final ErrorCode XQST0093 = W3CErrorCode.XQST0093.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0094}.
     */
    @Deprecated
    public static final ErrorCode XQST0094 = W3CErrorCode.XQST0094.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0101}.
     */
    @Deprecated
    public static final ErrorCode XQDY0101 = W3CErrorCode.XQDY0101.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0102}.
     */
    @Deprecated
    public static final ErrorCode XQDY0102 = W3CErrorCode.XQDY0102.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQST0103}.
     */
    @Deprecated
    public static final ErrorCode XQST0103 = W3CErrorCode.XQST0103.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0137}.
     */
    @Deprecated
    public static final ErrorCode XQDY0137 = W3CErrorCode.XQDY0137.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQDY0138}.
     */
    @Deprecated
    public static final ErrorCode XQDY0138 = W3CErrorCode.XQDY0138.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XUDY0023}.
     */
    @Deprecated
    public static final ErrorCode XUDY0023 = W3CErrorCode.XUDY0023.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOER0000}.
     */
    @Deprecated
    public static final ErrorCode FOER0000 = W3CErrorCode.FOER0000.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOAR0001}.
     */
    @Deprecated
    public static final ErrorCode FOAR0001 = W3CErrorCode.FOAR0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOAR0002}.
     */
    @Deprecated
    public static final ErrorCode FOAR0002 = W3CErrorCode.FOAR0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOCA0001}.
     */
    @Deprecated
    public static final ErrorCode FOCA0001 = W3CErrorCode.FOCA0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOCA0002}.
     */
    @Deprecated
    public static final ErrorCode FOCA0002 = W3CErrorCode.FOCA0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOCA0003}.
     */
    @Deprecated
    public static final ErrorCode FOCA0003 = W3CErrorCode.FOCA0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOCA0005}.
     */
    @Deprecated
    public static final ErrorCode FOCA0005 = W3CErrorCode.FOCA0005.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOCA0006}.
     */
    @Deprecated
    public static final ErrorCode FOCA0006 = W3CErrorCode.FOCA0006.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOCH0001}.
     */
    @Deprecated
    public static final ErrorCode FOCH0001 = W3CErrorCode.FOCH0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOCH0002}.
     */
    @Deprecated
    public static final ErrorCode FOCH0002 = W3CErrorCode.FOCH0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOCH0003}.
     */
    @Deprecated
    public static final ErrorCode FOCH0003 = W3CErrorCode.FOCH0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOCH0004}.
     */
    @Deprecated
    public static final ErrorCode FOCH0004 = W3CErrorCode.FOCH0004.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODC0001}.
     */
    @Deprecated
    public static final ErrorCode FODC0001 = W3CErrorCode.FODC0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODC0002}.
     */
    @Deprecated
    public static final ErrorCode FODC0002 = W3CErrorCode.FODC0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODC0003}.
     */
    @Deprecated
    public static final ErrorCode FODC0003 = W3CErrorCode.FODC0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODC0004}.
     */
    @Deprecated
    public static final ErrorCode FODC0004 = W3CErrorCode.FODC0004.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODC0005}.
     */
    @Deprecated
    public static final ErrorCode FODC0005 = W3CErrorCode.FODC0005.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODT0001}.
     */
    @Deprecated
    public static final ErrorCode FODT0001 = W3CErrorCode.FODT0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODT0002}.
     */
    @Deprecated
    public static final ErrorCode FODT0002 = W3CErrorCode.FODT0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODT0003}.
     */
    @Deprecated
    public static final ErrorCode FODT0003 = W3CErrorCode.FODT0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FONS0004}.
     */
    @Deprecated
    public static final ErrorCode FONS0004 = W3CErrorCode.FONS0004.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FONS0005}.
     */
    @Deprecated
    public static final ErrorCode FONS0005 = W3CErrorCode.FONS0005.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORG0001}.
     */
    @Deprecated
    public static final ErrorCode FORG0001 = W3CErrorCode.FORG0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORG0002}.
     */
    @Deprecated
    public static final ErrorCode FORG0002 = W3CErrorCode.FORG0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORG0003}.
     */
    @Deprecated
    public static final ErrorCode FORG0003 = W3CErrorCode.FORG0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORG0004}.
     */
    @Deprecated
    public static final ErrorCode FORG0004 = W3CErrorCode.FORG0004.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORG0005}.
     */
    @Deprecated
    public static final ErrorCode FORG0005 = W3CErrorCode.FORG0005.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORG0006}.
     */
    @Deprecated
    public static final ErrorCode FORG0006 = W3CErrorCode.FORG0006.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORG0008}.
     */
    @Deprecated
    public static final ErrorCode FORG0008 = W3CErrorCode.FORG0008.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORG0009}.
     */
    @Deprecated
    public static final ErrorCode FORG0009 = W3CErrorCode.FORG0009.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORG0010}.
     */
    @Deprecated
    public static final ErrorCode FORG0010 = W3CErrorCode.FORG0010.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORX0001}.
     */
    @Deprecated
    public static final ErrorCode FORX0001 = W3CErrorCode.FORX0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORX0002}.
     */
    @Deprecated
    public static final ErrorCode FORX0002 = W3CErrorCode.FORX0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORX0003}.
     */
    @Deprecated
    public static final ErrorCode FORX0003 = W3CErrorCode.FORX0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FORX0004}.
     */
    @Deprecated
    public static final ErrorCode FORX0004 = W3CErrorCode.FORX0004.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOTY0012}.
     */
    @Deprecated
    public static final ErrorCode FOTY0012 = W3CErrorCode.FOTY0012.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOTY0013}.
     */
    @Deprecated
    public static final ErrorCode FOTY0013 = W3CErrorCode.FOTY0013.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SENR0001}.
     */
    @Deprecated
    public static final ErrorCode SENR0001 = W3CErrorCode.SENR0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SERE0003}.
     */
    @Deprecated
    public static final ErrorCode SERE0003 = W3CErrorCode.SERE0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SEPM0004}.
     */
    @Deprecated
    public static final ErrorCode SEPM0004 = W3CErrorCode.SEPM0004.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SERE0005}.
     */
    @Deprecated
    public static final ErrorCode SERE0005 = W3CErrorCode.SERE0005.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SERE0006}.
     */
    @Deprecated
    public static final ErrorCode SERE0006 = W3CErrorCode.SERE0006.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SESU0007}.
     */
    @Deprecated
    public static final ErrorCode SESU0007 = W3CErrorCode.SESU0007.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SERE0008}.
     */
    @Deprecated
    public static final ErrorCode SERE0008 = W3CErrorCode.SERE0008.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SEPM0009}.
     */
    @Deprecated
    public static final ErrorCode SEPM0009 = W3CErrorCode.SEPM0009.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SEPM0010}.
     */
    @Deprecated
    public static final ErrorCode SEPM0010 = W3CErrorCode.SEPM0010.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SESU0011}.
     */
    @Deprecated
    public static final ErrorCode SESU0011 = W3CErrorCode.SESU0011.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SERE0012}.
     */
    @Deprecated
    public static final ErrorCode SERE0012 = W3CErrorCode.SERE0012.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SESU0013}.
     */
    @Deprecated
    public static final ErrorCode SESU0013 = W3CErrorCode.SESU0013.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SERE0014}.
     */
    @Deprecated
    public static final ErrorCode SERE0014 = W3CErrorCode.SERE0014.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SERE0015}.
     */
    @Deprecated
    public static final ErrorCode SERE0015 = W3CErrorCode.SERE0015.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SEPM0016}.
     */
    @Deprecated
    public static final ErrorCode SEPM0016 = W3CErrorCode.SEPM0016.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SEPM0017}.
     */
    @Deprecated
    public static final ErrorCode SEPM0017 = W3CErrorCode.SEPM0017.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SEPM0018}.
     */
    @Deprecated
    public static final ErrorCode SEPM0018 = W3CErrorCode.SEPM0018.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SEPM0019}.
     */
    @Deprecated
    public static final ErrorCode SEPM0019 = W3CErrorCode.SEPM0019.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#SERE0021}.
     */
    @Deprecated
    public static final ErrorCode SERE0021 = W3CErrorCode.SERE0021.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODF1280}.
     */
    @Deprecated
    public static final ErrorCode FODF1280 = W3CErrorCode.FODF1280.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODF1310}.
     */
    @Deprecated
    public static final ErrorCode FODF1310 = W3CErrorCode.FODF1310.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOFD1340}.
     */
    @Deprecated
    public static final ErrorCode FOFD1340 = W3CErrorCode.FOFD1340.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOFD1350}.
     */
    @Deprecated
    public static final ErrorCode FOFD1350 = W3CErrorCode.FOFD1350.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FTDY0020}.
     */
    @Deprecated
    public static final ErrorCode FTDY0020 = W3CErrorCode.FTDY0020.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FODC0006}.
     */
    @Deprecated
    public static final ErrorCode FODC0006 = W3CErrorCode.FODC0006.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOAP0001}.
     */
    @Deprecated
    public static final ErrorCode FOAP0001 = W3CErrorCode.FOAP0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XQTY0105}.
     */
    @Deprecated
    public static final ErrorCode XQTY0105 = W3CErrorCode.XQTY0105.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOAY0001}.
     */
    @Deprecated
    public static final ErrorCode FOAY0001 = W3CErrorCode.FOAY0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOAY0002}.
     */
    @Deprecated
    public static final ErrorCode FOAY0002 = W3CErrorCode.FOAY0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOJS0001}.
     */
    @Deprecated
    public static final ErrorCode FOJS0001 = W3CErrorCode.FOJS0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOJS0002}.
     */
    @Deprecated
    public static final ErrorCode FOJS0002 = W3CErrorCode.FOJS0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOJS0003}.
     */
    @Deprecated
    public static final ErrorCode FOJS0003 = W3CErrorCode.FOJS0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOJS0005}.
     */
    @Deprecated
    public static final ErrorCode FOJS0005 = W3CErrorCode.FOJS0005.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOJS0006}.
     */
    @Deprecated
    public static final ErrorCode FOJS0006 = W3CErrorCode.FOJS0006.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOJS0007}.
     */
    @Deprecated
    public static final ErrorCode FOJS0007 = W3CErrorCode.FOJS0007.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOUT1170}.
     */
    @Deprecated
    public static final ErrorCode FOUT1170 = W3CErrorCode.FOUT1170.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOUT1190}.
     */
    @Deprecated
    public static final ErrorCode FOUT1190 = W3CErrorCode.FOUT1190.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOUT1200}.
     */
    @Deprecated
    public static final ErrorCode FOUT1200 = W3CErrorCode.FOUT1200.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOQM0001}.
     */
    @Deprecated
    public static final ErrorCode FOQM0001 = W3CErrorCode.FOQM0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOQM0002}.
     */
    @Deprecated
    public static final ErrorCode FOQM0002 = W3CErrorCode.FOQM0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOQM0003}.
     */
    @Deprecated
    public static final ErrorCode FOQM0003 = W3CErrorCode.FOQM0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOQM0005}.
     */
    @Deprecated
    public static final ErrorCode FOQM0005 = W3CErrorCode.FOQM0005.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOQM0006}.
     */
    @Deprecated
    public static final ErrorCode FOQM0006 = W3CErrorCode.FOQM0006.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOXT0001}.
     */
    @Deprecated
    public static final ErrorCode FOXT0001 = W3CErrorCode.FOXT0001.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOXT0002}.
     */
    @Deprecated
    public static final ErrorCode FOXT0002 = W3CErrorCode.FOXT0002.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOXT0003}.
     */
    @Deprecated
    public static final ErrorCode FOXT0003 = W3CErrorCode.FOXT0003.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOXT0004}.
     */
    @Deprecated
    public static final ErrorCode FOXT0004 = W3CErrorCode.FOXT0004.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#FOXT0006}.
     */
    @Deprecated
    public static final ErrorCode FOXT0006 = W3CErrorCode.FOXT0006.errorCode;

    /**
     * @deprecated Use {@link W3CErrorCode#XTSE0165}.
     */
    @Deprecated
    public static final ErrorCode XTSE0165 = W3CErrorCode.XTSE0165.errorCode;

    /**
     * @deprecated Use {@link EXistErrorCode#EXXQDY0001}.
     */
    @Deprecated
    public static final ErrorCode EXXQDY0001 = EXistErrorCode.EXXQDY0001.errorCode;

    /**
     * @deprecated Use {@link EXistErrorCode#EXXQDY0002}.
     */
    @Deprecated
    public static final ErrorCode EXXQDY0002 = EXistErrorCode.EXXQDY0002.errorCode;

    /**
     * @deprecated Use {@link EXistErrorCode#EXXQDY0003}.
     */
    @Deprecated
    public static final ErrorCode EXXQDY0003 = EXistErrorCode.EXXQDY0003.errorCode;

    /**
     * @deprecated Use {@link EXistErrorCode#EXXQDY0004}.
     */
    @Deprecated
    public static final ErrorCode EXXQDY0004 = EXistErrorCode.EXXQDY0004.errorCode;

    /**
     * @deprecated Use {@link EXistErrorCode#EXXQDY0005}.
     */
    @Deprecated
    public static final ErrorCode EXXQDY0005 = EXistErrorCode.EXXQDY0005.errorCode;

    /**
     * @deprecated Use {@link EXistErrorCode#EXXQDY0006}.
     */
    @Deprecated
    public static final ErrorCode EXXQDY0006 = EXistErrorCode.EXXQDY0006.errorCode;

    /**
     * @deprecated Use {@link EXistErrorCode#EXXQST0001}.
     */
    @Deprecated
    public static final ErrorCode EXXQST0001 = EXistErrorCode.EXXQST0001.errorCode;

    /**
     * @deprecated Use {@link EXistErrorCode#EXXQST0002}.
     */
    @Deprecated
    public static final ErrorCode EXXQST0002 = EXistErrorCode.EXXQST0002.errorCode;

    /**
     * @deprecated Use {@link EXistErrorCode#EXXQST0003}.
     */
    @Deprecated
    public static final ErrorCode EXXQST0003 = EXistErrorCode.EXXQST0003.errorCode;

    /**
     * @deprecated Use {@link EXistErrorCode#ERROR}.
     */
    @Deprecated
    public static final ErrorCode ERROR = EXistErrorCode.ERROR.errorCode;
}
