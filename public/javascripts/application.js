/* global $ */
/* global jQuery */
/* global GOVUK */


$(document).ready(function () {
    // Turn off jQuery animation
    jQuery.fx.off = true;

    // Where .multiple-choice uses the data-target attribute
    // to toggle hidden content
    var showHideContent = new GOVUK.ShowHideContent();

    showHideContent.init()
});

window.onload = function () {
    if (document.getElementsByClassName("flash error-summary error-summary--show").length > 0) {
        ga('send', 'event', 'validationError', 'error', window.location.pathname);
    }

    if (window.location.href.indexOf("start") > -1) {
        if (document.getElementById("registration-status").textContent.indexOf("agent") > -1) {
            ga('send', 'event', 'agents', 'visited', 'agentsPageVisited');
        } else if (document.getElementById("registration-status").textContent.indexOf("assistant") > -1) {
            ga('send', 'event', 'assistants', 'visited', 'assistantsPageVisited');
        } else if (document.getElementById("registration-status").textContent.indexOf("already registered") > -1) {
            ga('send', 'event', 'alreadyRegistered', 'visited', 'alreadyRegisteredPageVisited');
        }

    } else if (window.location.href.indexOf("verify") > -1 && document.getElementById("registration-pending-title")) {
        ga('send', 'event', 'pending', 'visited', 'pendingPageVisited');
    }

};