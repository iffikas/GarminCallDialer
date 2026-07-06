import Toybox.Application;
import Toybox.WatchUi;
import Toybox.Lang;

class DialerApp extends Application.AppBase {

    function initialize() {
        AppBase.initialize();
    }

    function getInitialView() as [Views] or [Views, InputDelegates] {
        var favorites = FavoritesStore.loadFavorites();

        if (favorites.size() == 0) {
            var view = new MessageView(WatchUi.loadResource(Rez.Strings.NoFavorites) as String);
            return [view, new WatchUi.BehaviorDelegate()];
        }

        var menu = new WatchUi.Menu2({:title => WatchUi.loadResource(Rez.Strings.AppName) as String});
        for (var i = 0; i < favorites.size(); i++) {
            var favorite = favorites[i];
            menu.addItem(new WatchUi.MenuItem(favorite[:name], favorite[:number], favorite[:number], {}));
        }

        return [menu, new FavoritesMenuDelegate()];
    }
}
