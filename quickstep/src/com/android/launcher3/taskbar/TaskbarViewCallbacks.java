/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.taskbar;

import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_TASKBAR_ALLAPPS_BUTTON_LONG_PRESS;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_TASKBAR_ALLAPPS_BUTTON_TAP;

import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.internal.jank.Cuj;
import com.android.launcher3.taskbar.bubbles.BubbleBarViewController;
import com.android.systemui.shared.system.InteractionJankMonitorWrapper;
import com.android.wm.shell.Flags;
import com.android.wm.shell.shared.bubbles.BubbleBarLocation;

/**
 * Callbacks for {@link TaskbarView} to interact with its controller.
 */
public class TaskbarViewCallbacks {

    private final TaskbarActivityContext mActivity;
    private final TaskbarControllers mControllers;
    private final TaskbarView mTaskbarView;

    public TaskbarViewCallbacks(TaskbarActivityContext activity, TaskbarControllers controllers,
            TaskbarView taskbarView) {
        mActivity = activity;
        mControllers = controllers;
        mTaskbarView = taskbarView;
    }

    public View.OnClickListener getIconOnClickListener() {
        return mActivity.getItemOnClickListener();
    }

    /** Trigger All Apps button click action. */
    public void triggerAllAppsButtonClick(View v) {
        InteractionJankMonitorWrapper.begin(v, Cuj.CUJ_LAUNCHER_OPEN_ALL_APPS,
                /* tag= */ "TASKBAR_BUTTON");
        mActivity.getStatsLogManager().logger().log(LAUNCHER_TASKBAR_ALLAPPS_BUTTON_TAP);
        mControllers.taskbarAllAppsController.toggle();
    }

    /** Trigger All Apps button long click action. */
    public void triggerAllAppsButtonLongClick() {
        mActivity.getStatsLogManager().logger().log(LAUNCHER_TASKBAR_ALLAPPS_BUTTON_LONG_PRESS);
    }

    public boolean isAllAppsButtonHapticFeedbackEnabled() {
        return false;
    }

    public View.OnLongClickListener getTaskbarDividerLongClickListener() {
        return v -> {
            mControllers.taskbarPinningController.showPinningView(v);
            return true;
        };
    }

    /** Check to see if we support long press on taskbar divider */
    public boolean supportsDividerLongPress() {
        return !mActivity.isThreeButtonNav();
    }

    public View.OnTouchListener getTaskbarDividerRightClickListener() {
        return (v, event) -> {
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)
                    && event.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                mControllers.taskbarPinningController.showPinningView(v);
                return true;
            }
            return false;
        };
    }

    public View.OnLongClickListener getIconOnLongClickListener() {
        return mControllers.taskbarDragController::startDragOnLongClick;
    }

    /** Gets the hover listener for the provided icon view. */
    public View.OnHoverListener getIconOnHoverListener(View icon) {
        return new TaskbarHoverToolTipController(mActivity, mTaskbarView, icon);
    }

    /**
     * Notifies launcher to update icon alignment.
     */
    public void notifyIconLayoutBoundsChanged() {
        mControllers.uiController.onIconLayoutBoundsChanged();
    }

    /**
     * Notifies the taskbar scrim when the visibility of taskbar changes.
     */
    public void notifyVisibilityChanged() {
        mControllers.taskbarScrimViewController.onTaskbarVisibilityChanged(
                mTaskbarView.getVisibility());
    }

    /**
     * Get current location of bubble bar, if it is visible.
     * Returns {@code null} if bubble bar is not shown.
     */
    @Nullable
    public BubbleBarLocation getBubbleBarLocationIfVisible() {
        BubbleBarViewController bubbleBarViewController =
                mControllers.bubbleControllers.map(c -> c.bubbleBarViewController).orElse(null);
        if (bubbleBarViewController != null && bubbleBarViewController.isBubbleBarVisible()) {
            return bubbleBarViewController.getBubbleBarLocation();
        }
        return null;
    }

    /** Returns true if bubble bar controllers present and enabled in persistent taskbar. */
    public boolean isBubbleBarEnabledInPersistentTaskbar() {
        return Flags.enableBubbleBarInPersistentTaskBar()
                && mControllers.bubbleControllers.isPresent();
    }

    /** Returns on click listener for the taskbar overflow view. */
    public View.OnClickListener getOverflowOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mControllers.keyboardQuickSwitchController.openQuickSwitchView();
            }
        };
    }

    /** Returns on long click listener for the taskbar overflow view. */
    public View.OnLongClickListener getOverflowOnLongClickListener() {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mControllers.keyboardQuickSwitchController.openQuickSwitchView();
                return true;
            }
        };
    }
}
