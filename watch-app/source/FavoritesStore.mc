import Toybox.Application;
import Toybox.Application.Storage;
import Toybox.Lang;

// Favorites can come from two places:
//  1. Storage - saved on the watch after the phone app pushes an updated
//     list over BLE (see FavoritesSync). This takes priority.
//  2. Application Properties - the original build-time Name/Number slots
//     in resources/properties/properties.xml, used as a fallback until the
//     phone app has sent anything (or on `properties.xml`-based builds
//     that never enable the phone-side flow).
module FavoritesStore {

    function loadFavorites() as Array<Dictionary> {
        var stored = Storage.getValue("favorites") as Array?;
        if (stored != null && stored.size() > 0) {
            var fromStorage = normalizeStored(stored);
            if (fromStorage.size() > 0) {
                return fromStorage;
            }
        }
        return loadBuiltInFavorites();
    }

    // Same limit as the phone app (Constants.MAX_FAVORITES). ConnectIQ doesn't
    // authenticate which phone app sent a message, so anything stored here is
    // treated as untrusted: capped in size and type-checked before display.
    const MAX_FAVORITES = 5;

    function normalizeStored(stored as Array) as Array<Dictionary> {
        var favorites = [] as Array<Dictionary>;
        for (var i = 0; i < stored.size() && favorites.size() < MAX_FAVORITES; i++) {
            var entry = stored[i];
            if (!(entry instanceof Dictionary)) {
                continue;
            }
            var name = (entry as Dictionary).get("name");
            var number = (entry as Dictionary).get("number");
            if (name instanceof String && !name.equals("") && number instanceof String && !number.equals("")) {
                favorites.add({:name => name, :number => number});
            }
        }
        return favorites;
    }

    function loadBuiltInFavorites() as Array<Dictionary> {
        var favorites = [] as Array<Dictionary>;

        for (var i = 1; i <= 5; i++) {
            var name = Properties.getValue("Favorite" + i.toString() + "Name") as String?;
            var number = Properties.getValue("Favorite" + i.toString() + "Number") as String?;

            if (name != null && !name.equals("") && number != null && !number.equals("")) {
                favorites.add({:name => name, :number => number});
            }
        }

        return favorites;
    }
}
