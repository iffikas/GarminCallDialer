import Toybox.Application;
import Toybox.Application.Storage;
import Toybox.Communications;
import Toybox.Timer;
import Toybox.WatchUi;
import Toybox.Lang;

class DialerApp extends Application.AppBase {

    hidden var _syncTimer as Timer.Timer?;
    hidden var _awaitingResponse as Boolean;

    function initialize() {
        AppBase.initialize();
        _awaitingResponse = false;
    }

    function onStart(state as Dictionary?) as Void {
        Communications.registerForPhoneAppMessages(method(:onFavoritesMessage));
        // Always ask the phone for the current list on open, rather than
        // relying solely on whatever was last pushed - this way the watch
        // shows cached favorites immediately but self-corrects if they've
        // changed since.
        requestFavoritesFromPhone();
    }

    function getInitialView() as [Views] or [Views, InputDelegates] {
        return buildFavoritesView();
    }

    function requestFavoritesFromPhone() as Void {
        cancelSyncTimer();
        _awaitingResponse = true;

        Communications.transmit({"cmd" => "get_favorites"}, null, new RequestFavoritesListener());

        _syncTimer = new Timer.Timer();
        _syncTimer.start(method(:onSyncTimeout), 6000, false);
    }

    // Called by RequestFavoritesListener if the request couldn't even be
    // delivered to the phone (e.g. watch/phone not connected over BLE).
    function onRequestFailed() as Void {
        if (!_awaitingResponse) {
            return;
        }
        cancelSyncTimer();
        _awaitingResponse = false;
        showNotSyncedIfEmpty();
    }

    // Fires if the phone never responds at all - delivered over BLE but the
    // companion app wasn't running to answer, for example.
    function onSyncTimeout() as Void {
        if (!_awaitingResponse) {
            return;
        }
        _awaitingResponse = false;
        showNotSyncedIfEmpty();
    }

    hidden function showNotSyncedIfEmpty() as Void {
        // Only interrupt with an error state if we have nothing cached to
        // show - if we already have favorites from a previous sync, just
        // keep showing them.
        var favorites = FavoritesStore.loadFavorites();
        if (favorites.size() == 0) {
            var view = new MessageView(WatchUi.loadResource(Rez.Strings.NotSynced) as String);
            WatchUi.switchToView(view, new NotSyncedDelegate(), WatchUi.SLIDE_IMMEDIATE);
        }
    }

    hidden function cancelSyncTimer() as Void {
        if (_syncTimer != null) {
            _syncTimer.stop();
            _syncTimer = null;
        }
    }

    // Receives an updated favorites list pushed from the Android companion
    // app - either in response to requestFavoritesFromPhone() above, or an
    // unprompted push from the phone's Manage favorites screen - and
    // persists it to on-device Storage, where FavoritesStore picks it up
    // ahead of the build-time Application Properties defaults.
    //
    // Expected message shape:
    // {"favorites" => [{"name" => "...", "number" => "..."}, ...]}
    function onFavoritesMessage(msg as Communications.PhoneAppMessage) as Void {
        cancelSyncTimer();
        _awaitingResponse = false;

        // Temporary on-screen diagnostics for the "phone says sent, watch
        // shows no favorites" issue - shown as an extra line under
        // "No favorites yet" until we track down where the message is
        // getting lost or coming back empty.
        var payload = msg as Dictionary?;
        if (payload == null) {
            Storage.setValue("debugLastSync", "msg not Dict: " + msg.toString());
            refreshView();
            return;
        }

        var list = payload.get("favorites") as Array?;
        if (list == null) {
            Storage.setValue("debugLastSync", "no 'favorites' key: " + payload.toString());
            refreshView();
            return;
        }

        Storage.setValue("debugLastSync", "raw:" + list.size().toString() + " " + list.toString());
        Storage.setValue("favorites", list);
        refreshView();
    }

    hidden function refreshView() as Void {
        var view = buildFavoritesView();
        WatchUi.switchToView(view[0], view[1], WatchUi.SLIDE_IMMEDIATE);
    }

    function buildFavoritesView() as [Views, InputDelegates] {
        var favorites = FavoritesStore.loadFavorites();

        if (favorites.size() == 0) {
            var text = WatchUi.loadResource(Rez.Strings.NoFavorites) as String;
            var debugInfo = Storage.getValue("debugLastSync") as String?;
            if (debugInfo != null) {
                text = text + "\n\n" + debugInfo;
            }
            var view = new MessageView(text);
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

// Only confirms the request reached the phone over BLE - the actual
// favorites (if any) arrive separately via onFavoritesMessage above.
class RequestFavoritesListener extends Communications.ConnectionListener {

    function initialize() {
        Communications.ConnectionListener.initialize();
    }

    function onComplete() as Void {
    }

    function onError() as Void {
        var app = Application.getApp() as DialerApp;
        app.onRequestFailed();
    }
}

// Lets the user retry the phone sync from the "Not synced" error view by
// pressing the primary/select button.
class NotSyncedDelegate extends WatchUi.BehaviorDelegate {

    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onSelect() as Boolean {
        var app = Application.getApp() as DialerApp;
        app.requestFavoritesFromPhone();
        return true;
    }
}
