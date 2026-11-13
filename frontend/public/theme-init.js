// Runs before the page paints, so the saved or system theme is applied without a flash.
(function () {
    var theme = 'dark';
    try {
        var saved = localStorage.getItem('theme');
        if (saved === 'light' || saved === 'dark') {
            theme = saved;
        } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) {
            theme = 'light';
        }
    } catch (e) {
        // Storage can be unavailable (private mode); fall back to the system setting.
    }
    document.documentElement.setAttribute('data-theme', theme);
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', theme === 'light' ? '#f4f3ee' : '#0f1115');
})();
