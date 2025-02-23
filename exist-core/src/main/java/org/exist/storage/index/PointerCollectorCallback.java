package org.exist.storage.index;

import org.exist.storage.btree.BTreeCallback;
import org.exist.storage.btree.Value;
import org.exist.xquery.TerminatedException;

import java.util.Arrays;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
class PointerCollectorCallback implements BTreeCallback {
    long[] pointers = new long[128];
    int count = 0;

    @Override
    public boolean indexInfo(final Value value, final long pointer) throws TerminatedException {
        if (this.count == this.pointers.length) {
            this.pointers = Arrays.copyOf(this.pointers, this.count * 2);
        }
        this.pointers[this.count++] = pointer;
        return true;
    }
}
