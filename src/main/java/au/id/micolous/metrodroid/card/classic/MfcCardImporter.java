package au.id.micolous.metrodroid.card.classic;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;

import au.id.micolous.metrodroid.card.CardImporter;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

public class MfcCardImporter implements CardImporter<ClassicCard> {
    private static final int MAX_SECTORS = 40;

    @NonNull
    @Override
    public ClassicCard readCard(@NonNull InputStream stream) throws IOException {
        // Read the blocks of the card.
        ArrayList<ClassicSector> sectors = new ArrayList<>();
        byte[] uid = null;
        int maxSector = 0;

    sectorloop:
        for (int sectorNum=0; sectorNum<MAX_SECTORS; sectorNum++) {
            ArrayList<ClassicBlock> blocks = new ArrayList<>();
            byte[] key = null;

            int blockCount = 4;
            if (sectorNum >= 32) {
                blockCount = 16;
            }

            for (int blockNum=0; blockNum<blockCount; blockNum++) {
                byte[] blockData = new byte[16];
                int r = stream.read(blockData);
                if (r <= 0 && blockNum == 0) {
                    // We got to the end of the file.
                    break sectorloop;
                } else if (r != blockData.length) {
                    throw new IOException(String.format(Locale.ENGLISH,
                            "Incomplete MFC read at sector %d block %d (%d bytes)",
                            sectorNum, blockNum, r));
                }

                if (sectorNum == 0 && blockNum == 0) {
                    // Manufacturer data
                    uid = ArrayUtils.subarray(blockData, 0, 4);
                    blocks.add(new ClassicBlock(blockNum, ClassicBlock.TYPE_MANUFACTURER, blockData));
                } else if (blockNum == blockCount - 1) {
                    key = ArrayUtils.subarray(blockData, 0, 6);
                    blocks.add(new ClassicBlock(blockNum, ClassicBlock.TYPE_TRAILER, blockData));
                } else {
                    blocks.add(new ClassicBlock(blockNum, ClassicBlock.TYPE_DATA, blockData));
                }
            }

            assert key != null;
            sectors.add(new ClassicSector(sectorNum, blocks.toArray(new ClassicBlock[0]),
                    ClassicSectorKey.Companion.fromDump(ImmutableByteArray.Companion.fromByteArray(key),
                            ClassicSectorKey.KeyType.A, "mfc-dump")));
            maxSector = sectorNum;
        }

        // End of file, now see how many blocks we get
        if (maxSector <= 15) {
            maxSector = 15; // 1K
        } else if (maxSector <= 31) {
            maxSector = 31; // 2K
        } else if (maxSector <= 39) {
            maxSector = 39; // 4K
        }

        // Fill missing sectors as "unauthorised".
        while (sectors.size() <= maxSector) {
            sectors.add(new UnauthorizedClassicSector(sectors.size()));
        }

        return new ClassicCard(uid,
                GregorianCalendar.getInstance(), sectors, false);
    }
}
