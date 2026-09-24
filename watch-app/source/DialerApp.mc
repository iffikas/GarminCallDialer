import Toybox.Application;
import Toybox.Application.Storage;
import Toybox.Communications;
import Toybox.WatchUi;
import Toybox.Lang;

class DialerApp extends Application.AppBase {

    function initialize() {
        AppBase.initialize();
    }

    function onStart(state as Dictionary?) as Void {
        Communications.registerForPhoneAppMessages(method(:onFavoritesMessage));
    }

    function getInitialView() as [Views] or [Views, InputDelegates] {
        return buildFavoritesView();
    }

    // Receives an updated favorites list pushed from the Android companion
    // app (its FavoritesActivity lets you pick contacts or type
    // names/numbers) and persists it to on-device Storage, where
    // FavoritesStore picks it up ahead of the build-time Application
    // Properties defaults.
    //
    // Expected message shape:
    // {"favorites" => [{"name" => "...", "number" => "..."}, ...]}
    function onFavoritesMessage(msg as Communications.PhoneAppMessage) as Void {
        var payload = msg as Dictionary?;
        if (payload == null) {
            return;
        }

        var list = payload.get("favorites") as Array?;
        if (list == null) {
            return;
        }

        Storage.setValue("favorites", list);

        // Refresh immediately if the favorites menu is already on screen,
        // rather than requiring the user to close and reopen the widget.
        var view = buildFavoritesView();
        WatchUi.switchToView(view[0], view[1], WatchUi.SLIDE_IMMEDIATE);
    }

    function buildFavoritesView() as [Views, InputDelegates] {
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
