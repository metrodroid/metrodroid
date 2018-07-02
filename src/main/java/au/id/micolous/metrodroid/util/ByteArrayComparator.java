package au.id.micolous.metrodroid.util;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by michael on 2/07/18.
 */

public class ByteArrayComparator implements Comparator<byte[]> {
    @Override
    public int compare(byte[] o1, byte[] o2) {
        return Arrays.equals(o1, o2) ? 0 : 1;
    }
}
