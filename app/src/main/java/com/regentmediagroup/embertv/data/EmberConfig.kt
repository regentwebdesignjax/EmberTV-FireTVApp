package com.regentmediagroup.embertv.data

object EmberConfig {
    /** The Ember TV web app. Every /v2 route lives under it, and viewers
     *  approve this TV at <API_BASE_URL>activate. */
    const val API_BASE_URL = "https://app.emberstreaming.com/"

    /** Supabase project, used only to refresh and end the sign-in session. */
    const val SUPABASE_URL = "https://bqdoxfeuhfzljvddpjbd.supabase.co/"

    /** Supabase publishable key. Public by design (the website ships the
     *  same one); it identifies the project, it does not grant access. */
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_dDyGlDF2w0gvX1bJbz_knw_xVt4U6Lp"

    /** Sent as `client` on activation, playback and progress. */
    const val CLIENT = "firetv"

    /** Shown on screen, so no scheme. */
    const val WEBSITE_DISPLAY_NAME = "app.emberstreaming.com"
}
