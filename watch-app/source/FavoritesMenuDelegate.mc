import Toybox.WatchUi;
import Toybox.Lang;

class FavoritesMenuDelegate extends WatchUi.Menu2InputDelegate {

    function initialize() {
        Menu2InputDelegate.initialize();
    }

    function onSelect(item as WatchUi.MenuItem) as Void {
        var number = item.getId() as String;
        var name = item.getLabel() as String;
        CallTransmitter.sendCallRequest(number, name);
    }
}
