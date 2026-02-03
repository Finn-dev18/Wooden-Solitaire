public interface GameUIController {
    void requestLaserDirection();
    void onModelUpdated();
    void requestMenu();
    void requestRestart();
    boolean isOverlayActive();
    OverlayState getOverlayState();
}
