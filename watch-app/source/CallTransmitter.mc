import Toybox.Communications;
import Toybox.Timer;
import Toybox.WatchUi;
import Toybox.Lang;

// Sends {"n" => "<number>"} to the paired Android companion app, which
// dials it immediately via TelecomManager. This only confirms the
// message reached the phone over BLE - the actual call state (ringing,
// connected, ended) happens on the phone and isn't reported back for v1.
module CallTransmitter {

    // Held here so the listener (and the timer it owns) survives until the
    // confirmation message has been on screen long enough to read.
    var activeListener as CallTransmitListener?;

    function sendCallRequest(number as String, name as String) as Void {
        activeListener = new CallTransmitListener();
        Communications.transmit({"n" => number}, null, activeListener);

        var sendingText = Lang.format(
            WatchUi.loadResource(Rez.Strings.Sending) as String,
            [name]
        );
        WatchUi.pushView(new MessageView(sendingText), new WatchUi.BehaviorDelegate(), WatchUi.SLIDE_IMMEDIATE);
    }
}

class CallTransmitListener extends Communications.ConnectionListener {

    hidden const CONFIRMATION_MS = 3000;

    hidden var _dismissTimer as Timer.Timer?;

    function initialize() {
        Communications.ConnectionListener.initialize();
    }

    function onComplete() as Void {
        // Delivered to the companion app - it's now responsible for dialing.
        // Hold the confirmation on screen for a few seconds so it's readable,
        // then drop back to the favorites menu.
        var placedText = WatchUi.loadResource(Rez.Strings.CallPlaced) as String;
        WatchUi.switchToView(new MessageView(placedText), new WatchUi.BehaviorDelegate(), WatchUi.SLIDE_IMMEDIATE);

        _dismissTimer = new Timer.Timer();
        _dismissTimer.start(method(:onConfirmationShown), CONFIRMATION_MS, false);
    }

    function onConfirmationShown() as Void {
        _dismissTimer = null;
        CallTransmitter.activeListener = null;
        WatchUi.popView(WatchUi.SLIDE_IMMEDIATE);
    }

    function onError() as Void {
        var failedText = WatchUi.loadResource(Rez.Strings.SendFailed) as String;
        WatchUi.switchToView(new MessageView(failedText), new WatchUi.BehaviorDelegate(), WatchUi.SLIDE_IMMEDIATE);
    }
}
