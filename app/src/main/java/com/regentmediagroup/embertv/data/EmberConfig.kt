package com.regentmediagroup.embertv.data

import com.regentmediagroup.embertv.BuildConfig

object EmberConfig {
    /** True in the "staging" build type: the staging site and Supabase
     *  project, paid with Stripe test cards. Store builds are never staging. */
    val IS_STAGING = BuildConfig.STAGING

    /** The Ember TV web app. Every /v2 route lives under it, and viewers
     *  approve this TV at <API_BASE_URL>activate. */
    val API_BASE_URL =
        if (IS_STAGING) "https://staging--embertv.netlify.app/" else "https://app.emberstreaming.com/"

    /** Supabase project, used only to refresh and end the sign-in session. */
    val SUPABASE_URL =
        if (IS_STAGING) "https://tfnyowkprvmmkyckipya.supabase.co/"
        else "https://bqdoxfeuhfzljvddpjbd.supabase.co/"

    /** Supabase publishable key. Public by design (the website ships the
     *  same one); it identifies the project, it does not grant access. */
    val SUPABASE_PUBLISHABLE_KEY =
        if (IS_STAGING) "sb_publishable_P8c7Sb8DX51GiTuWA6GEPA_0XB-WrIu"
        else "sb_publishable_dDyGlDF2w0gvX1bJbz_knw_xVt4U6Lp"

    /** Sent as `client` on activation, playback and progress. */
    const val CLIENT = "firetv"

    /** Shown on screen, so no scheme. */
    val WEBSITE_DISPLAY_NAME =
        if (IS_STAGING) "staging--embertv.netlify.app" else "app.emberstreaming.com"
}
