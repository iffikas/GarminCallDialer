import Toybox.Communications;
import Toybox.WatchUi;
import Toybox.Lang;

// Sends {"n" => "<number>"} to the paired Android companion app, which
// dials it immediately via Intent.ACTION_CALL. This only confirms the
// message reached the phone over BLE - the actual call state (ringing,
// connected, ended) happens on the phone and isn't reported back for v1.
module CallTransmitter {

    function sendCallRequest(number as String, name as String) as Void {
        var listener = new CallTransmitListener();
        Communications.transmit({"n" => number}, null, listener);

        var sendingText = Lang.format(
            WatchUi.loadResource(Rez.Strings.Sending) as String,
            [name]
        );
        WatchUi.pushView(new MessageView(sendingText), new WatchUi.BehaviorDelegate(), WatchUi.SLIDE_IMMEDIATE);
    }
}

class CallTransmitListener extends Communications.ConnectionListener {

    function initialize() {
        Communications.ConnectionListener.initialize();
    }

    function onComplete() as Void {
        // Delivered to the companion app - it's now responsible for dialing.
    }

    function onError() as Void {
        var failedText = WatchUi.loadResource(Rez.Strings.SendFailed) as String;
        WatchUi.switchToView(new MessageView(failedText), new WatchUi.BehaviorDelegate(), WatchUi.SLIDE_IMMEDIATE);
    }
}
