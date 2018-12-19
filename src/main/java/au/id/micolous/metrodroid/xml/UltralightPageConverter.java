package au.id.micolous.metrodroid.xml;

import au.id.micolous.metrodroid.card.ultralight.UltralightPage;
import au.id.micolous.metrodroid.card.ultralight.UnauthorizedUltralightPage;
import au.id.micolous.metrodroid.util.Utils;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * Converts MIFARE Ultralight page types for the XML serialiser.
 */

public class UltralightPageConverter implements Converter<UltralightPage> {
    @Override
    public UltralightPage read(InputNode node) throws Exception {
        int pageIndex = Integer.parseInt(node.getAttribute("index").getValue());

        if (Utils.getBooleanAttr(node, "unauthorized")) {
            return new UnauthorizedUltralightPage(pageIndex);
        }

        throw new SkippableRegistryStrategy.SkipException();
    }

    @Override
    public void write(OutputNode node, UltralightPage value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}
