import Toybox.WatchUi;
import Toybox.Graphics;
import Toybox.Lang;

class MessageView extends WatchUi.View {
    hidden var _text as String;

    // Gap kept between the text and the subscreen's ring, and the bottom edge.
    hidden const MARGIN = 6;

    function initialize(text as String) {
        View.initialize();
        _text = text;
    }

    function onUpdate(dc as Graphics.Dc) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();
        var layout = textLayout(dc);
        dc.drawText(
            dc.getWidth() / 2,
            layout[1] as Number,
            layout[0] as Graphics.FontDefinition,
            _text,
            Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
        );
    }

    // Returns [font, centre y]. Devices with a subscreen (Instinct 2 family)
    // have a round window in the top-right corner that covers part of the
    // main display, so centred text partly disappears under it. There the
    // text goes below the subscreen instead, in the largest font that still
    // fits above the bottom edge (four-line messages don't fit in medium on
    // a 176px screen). Everything else stays centred in medium.
    hidden function textLayout(dc as Graphics.Dc) as Array {
        var height = dc.getHeight();
        var centered = [Graphics.FONT_MEDIUM, height / 2];
        if (!(WatchUi has :getSubscreen)) {
            return centered;
        }
        var subscreen = WatchUi.getSubscreen();
        if (subscreen == null) {
            return centered;
        }

        var top = subscreen.y + subscreen.height + MARGIN;
        var bottom = height - MARGIN;
        var fonts = [Graphics.FONT_MEDIUM, Graphics.FONT_SMALL, Graphics.FONT_TINY] as Array<Graphics.FontDefinition>;
        for (var i = 0; i < fonts.size(); i++) {
            var textHeight = dc.getTextDimensions(_text, fonts[i])[1];
            if (top + textHeight <= bottom) {
                var centerY = top + textHeight / 2;
                // Already clear of the subscreen when centred: keep it centred.
                return [fonts[i], centerY > height / 2 ? centerY : height / 2];
            }
        }
        return [Graphics.FONT_TINY, (top + bottom) / 2];
    }
}
