package com.blazebrowser.bridge

import android.content.Context
import android.content.SharedPreferences

data class BlazeLinkSite(
    val domain: String,
    val displayName: String,
    val modelSelectorJs: String,
    val inputSelectorJs: String,
    val submitSelectorJs: String,
    val enabled: Boolean = true
)

class BlazeLinkManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blaze_link", Context.MODE_PRIVATE)
    private val sites = mutableMapOf<String, BlazeLinkSite>()

    init {
        sites["chat.qwen.ai"] = BlazeLinkSite(
            domain = "chat.qwen.ai",
            displayName = "Qwen Chat",
            modelSelectorJs = "document.querySelector('[class*=\"model\"], [class*=\"Model\"]')?.click() || document.querySelector('select')?.click()",
            inputSelectorJs = "document.querySelector('[contenteditable=\"true\"], textarea, [role=\"textbox\"]')",
            submitSelectorJs = "document.querySelector('button[type=\"submit\"], [class*=\"send\"], [class*=\"Send\"]')"
        )
        sites["chat.openai.com"] = BlazeLinkSite(
            domain = "chat.openai.com",
            displayName = "ChatGPT",
            modelSelectorJs = "div[role='button'][id*='model']",
            inputSelectorJs = "#prompt-textarea, textarea[tabindex='0']",
            submitSelectorJs = "button[data-testid='send-button']"
        )
        sites["gemini.google.com"] = BlazeLinkSite(
            domain = "gemini.google.com",
            displayName = "Gemini",
            modelSelectorJs = "model-selector, [class*='model-picker']",
            inputSelectorJs = "[contenteditable='true'], textarea",
            submitSelectorJs = "[aria-label='Send'], button[aria-label='Submit']"
        )
        sites["kimi.moonshot.cn"] = BlazeLinkSite(
            domain = "kimi.moonshot.cn",
            displayName = "Kimi",
            modelSelectorJs = "[class*='model']",
            inputSelectorJs = "[contenteditable='true'], textarea",
            submitSelectorJs = "[class*='send'], button[type='submit']"
        )
    }

    fun getSites(): List<BlazeLinkSite> = sites.values.toList()
    fun getSite(domain: String): BlazeLinkSite? = sites[domain]
    fun isEnabled(domain: String): Boolean = prefs.getBoolean("enabled_$domain", false)
    fun setEnabled(domain: String, enabled: Boolean) = prefs.edit().putBoolean("enabled_$domain", enabled).apply()

    fun getInjectionScript(modelId: String): String {
        return """
            (function() {
                if (window.__blaze_link_injected) return 'already_injected';
                window.__blaze_link_injected = true;
                window.__blaze_model_id = '$modelId';

                // Add BlazeAI to model selector
                function addBlazeAIModel() {
                    // Try various model selector patterns
                    var selectors = [
                        '[class*="model"]', '[class*="Model"]', 'select',
                        '[class*="model-picker"]', 'model-selector',
                        'div[role="button"]', '[class*="dropdown"]'
                    ];

                    for (var i = 0; i < selectors.length; i++) {
                        var els = document.querySelectorAll(selectors[i]);
                        els.forEach(function(el) {
                            if (!el.querySelector('[data-blaze-ai]')) {
                                var option = document.createElement('div');
                                option.setAttribute('data-blaze-ai', 'true');
                                option.textContent = '⚡ BlazeAI';
                                option.style.cssText = 'padding:8px;cursor:pointer;color:#FF6B00;font-weight:bold;';
                                option.onclick = function() {
                                    window.__blaze_selected = true;
                                    el.textContent = '⚡ BlazeAI';
                                };
                                el.appendChild(option);
                            }
                        });
                    }
                }

                // Intercept form submissions
                function interceptSubmit() {
                    var origFetch = window.fetch;
                    window.fetch = function() {
                        var url = arguments[0];
                        var opts = arguments[1] || {};
                        if (url.toString().includes('/conversation') || url.toString().includes('/api/')) {
                            var body = opts.body ? JSON.parse(opts.body) : {};
                            if (window.__blaze_selected) {
                                body.model = window.__blaze_model_id;
                                // Send to BlazeAI bridge
                                opts.body = JSON.stringify(body);
                            }
                        }
                        return origFetch.apply(this, arguments);
                    };
                }

                // Check if we're on a chat page and inject
                if (document.querySelector('textarea, [contenteditable="true"], [role="textbox"]')) {
                    addBlazeAIModel();
                }

                // Re-inject on navigation
                var observer = new MutationObserver(function(mutations) {
                    if (document.querySelector('textarea, [contenteditable="true"]')) {
                        addBlazeAIModel();
                    }
                });
                observer.observe(document.body, { childList: true, subtree: true });

                return 'injected';
            })();
        """.trimIndent()
    }
}
