package au.id.micolous.metrodroid.card.classic;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.id.micolous.metrodroid.card.CardImporter;
import au.id.micolous.metrodroid.key.ClassicSectorKey;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

/**
 * Class to read files built by MIFARE Classic Tool.
 */
public class MctCardImporter implements CardImporter.Text<ClassicCard> {
    @Nullable
    @Override
    public ClassicCard readCard(@NonNull Reader reader) throws Exception {
        BufferedReader r = new BufferedReader(reader);

        List<ClassicSector> sectors = new ArrayList<>();
        int curSector = -1;
        int maxSector = -1;
        int blockNumber = 0;
        List <ClassicBlock> curBlocks = null;
        String lastBlock = null;
        String line = r.readLine();
        while (line != null) {

            if (line.startsWith("+Sector:")) {
                flushSector(sectors, curSector, curBlocks, lastBlock);
                curBlocks = new ArrayList<>();
                curSector = Integer.valueOf(line.substring(8).trim());
                if (curSector > maxSector)
                    maxSector = curSector;
                blockNumber = 0;
            } else {
                if (curBlocks != null && curSector >= 0) {
                    lastBlock = line;
                    curBlocks.add(new ClassicBlock(blockNumber, ClassicBlock.TYPE_DATA,
                            ImmutableByteArray.Companion.fromHex(line.replaceAll("-", "0"))));
                    blockNumber++;
                }
            }

            line = r.readLine();
        }

        flushSector(sectors, curSector, curBlocks, lastBlock);
        ImmutableByteArray uid;
        if (sectors.get(0) != null) {
            ImmutableByteArray block0 = sectors.get(0).getBlock(0).getData();
            if (block0.get(0) == 4)
                uid = block0.copyOfRange(0, 7);
            else
                uid = block0.copyOfRange(0, 4);
        } else
            uid = ImmutableByteArray.Companion.fromASCII("fake");

        if (maxSector <= 15)
            maxSector = 15; // 1K
        else if (maxSector <= 31)
            maxSector = 31; // 2K
        else if (maxSector <= 39)
            maxSector = 39; // 4K

        Set<Integer> s = new HashSet<>();
        for (ClassicSector sec : sectors)
            s.add(sec.getIndex());

        for (int i = 0; i <= maxSector; i++)
            if (!s.contains(i))
                sectors.add(new UnauthorizedClassicSector(i));

        Collections.sort(sectors, (a, b) -> Integer.compare(a.getIndex(), b.getIndex()));

        return new ClassicCard(uid,
                GregorianCalendar.getInstance(), sectors, false);
    }


    private void flushSector(List<ClassicSector> sectors, int curSector, List<ClassicBlock> curBlocks, String lastBlock) {
        if (curSector < 0 || curBlocks == null)
            return;
        ClassicSectorKey key = null;
        if (! lastBlock.startsWith("-")) {
            key = ClassicSectorKey.Companion.fromDump(
                    ImmutableByteArray.Companion.fromHex(lastBlock.substring(0, 12)),
                    ClassicSectorKey.KeyType.A, "mct-dump");
        } else {
            key = ClassicSectorKey.Companion.fromDump(
                    ImmutableByteArray.Companion.fromHex(lastBlock.substring(20, 32)),
                    ClassicSectorKey.KeyType.B, "mct-dump");
        }
        sectors.add(new ClassicSector(curSector, curBlocks.toArray(new ClassicBlock[0]), key));
    }
}
