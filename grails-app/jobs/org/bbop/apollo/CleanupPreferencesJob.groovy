package org.bbop.apollo


class CleanupPreferencesJob {

    def preferenceService

    static triggers = {
//      simple repeatInterval: 30000l // execute job every 30 seconds for testing
        simple repeatInterval: 24 * 60 * 60 * 1000l // execute job once a day
    }

    def execute() {
        // execute job
        preferenceService.removeStalePreferences()
    }
}
