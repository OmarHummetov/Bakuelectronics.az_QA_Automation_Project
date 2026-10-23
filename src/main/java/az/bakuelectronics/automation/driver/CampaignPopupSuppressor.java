package az.bakuelectronics.automation.driver;

import az.bakuelectronics.automation.config.ConfigReader;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.Map;


final class CampaignPopupSuppressor {

    private static final String SCRIPT = """
            (() => {
              const MODAL_SELECTOR = '.campaignModal.modal';
              const CLOSE_SELECTOR = 'button.campaignModalClose';
              const STYLE_ID = '__baku_campaign_popup_suppression__';

              const installStyle = () => {
                if (document.getElementById(STYLE_ID)) return;
                const style = document.createElement('style');
                style.id = STYLE_ID;
                style.textContent = MODAL_SELECTOR +
                  ' { visibility: hidden !important; pointer-events: none !important; }';
                (document.head || document.documentElement).appendChild(style);
              };

              const removeStyleIfUnused = () => {
                if (!document.querySelector(MODAL_SELECTOR)) {
                  document.getElementById(STYLE_ID)?.remove();
                }
              };

              const handleModal = (modal) => {
                if (!(modal instanceof Element) || !modal.matches(MODAL_SELECTOR)) return;
                installStyle();
                const closeButton = modal.querySelector(CLOSE_SELECTOR);
                if (closeButton && !modal.dataset.bakuCampaignCloseRequested) {
                  modal.dataset.bakuCampaignCloseRequested = 'true';
                  closeButton.click();
                  queueMicrotask(removeStyleIfUnused);
                }
              };

              const scan = (root) => {
                if (root instanceof Element) handleModal(root);
                root.querySelectorAll?.(MODAL_SELECTOR).forEach(handleModal);
              };

              const start = () => {
                installStyle();
                scan(document);
                new MutationObserver((mutations) => {
                  mutations.forEach((mutation) => {
                    mutation.addedNodes.forEach(scan);
                    if (mutation.type === 'attributes') scan(mutation.target);
                  });
                  removeStyleIfUnused();
                }).observe(document.documentElement, {
                  childList: true,
                  subtree: true,
                  attributes: true,
                  attributeFilter: ['class']
                });
              };

              if (document.documentElement) start();
              else document.addEventListener('DOMContentLoaded', start, { once: true });
            })();
            """;

    private CampaignPopupSuppressor() {
    }

    static void installIfEnabled(ChromeDriver driver) {
        if (ConfigReader.isCampaignPopupSuppressionEnabled()) {
            driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", SCRIPT));
        }
    }
}
