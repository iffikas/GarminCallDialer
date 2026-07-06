import Toybox.Application;
import Toybox.Lang;

// Reads the 5 favorite Name/Number slots from Application Settings
// (edited via the native Garmin Connect Mobile app-settings screen)
// and returns only the slots that have both a name and a number set.
module FavoritesStore {

    function loadFavorites() as Array<Dictionary> {
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
