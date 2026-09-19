package com.moneymanager

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.moneymanager.ui.MoneyManagerTheme
import com.moneymanager.ui.MoneyManagerApp

/*
 * DIRECTION CONTRACT -- Money Manager, Android, all surfaces.
 *
 * THESIS: Money is a level in a column of water, not a stack of metric cards. This app refuses
 *   the fintech dashboard's balance-plus-stat-chips header: the figure and the field are the
 *   same fact, read two ways.
 * OWN-WORLD: A depth ladder from #17406C down to #04101D. Translucent plates lit only along
 *   their top edge, denser the deeper they sit. Archivo run wide (wdth 112-118) for every
 *   figure that stands for real money, tabular always, cents shown but set at 64%. One warm
 *   hue, #F6C566, reserved for goals and streaks and nothing else.
 * STORY: The user sees how much month is left before they see anything else, logs a spend in
 *   one tap from any screen, and trusts the arithmetic enough to keep logging.
 * FIRST VIEWPORT: Full-bleed water column. Safe-to-spend at 54sp sitting over the surface line;
 *   the fill height is that figure. Remaining days as a depth gauge down the right edge.
 *   Today's logged strip beneath it. One FAB, bottom-right, that logs.
 * FORM: Pinned by the user to "UI sample/UI 5.jpg" (deep blue glass). No direction roll -- a
 *   brief-pinned direction beats the roll. Ordered list not run; the pin settled it.
 * FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review,
 *   the verdict, DESIGN.md, and every shipping raster carrying its provenance.
 */

/**
 * A [FragmentActivity] rather than a plain ComponentActivity, because [androidx.biometric
 * .BiometricPrompt] hosts itself in a fragment and will not attach to anything less.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Blanks the thumbnail Android takes for the app switcher, and blocks screenshots while
        // it is set. Read once at start: toggling it mid-session would need the window recreated.
        if ((application as MoneyApp).security.hideInRecents) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContent {
            MoneyManagerTheme {
                MoneyManagerApp()
            }
        }
    }
}
