/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.net.module.util;

import static android.content.pm.PackageManager.MATCH_SYSTEM_ONLY;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.provider.DeviceConfig;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;

import java.util.Arrays;


/**
 * Tests for DeviceConfigUtils.
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeviceConfigUtilsTest {
    private static final String TEST_NAME_SPACE = "connectivity";
    private static final String TEST_EXPERIMENT_FLAG = "experiment_flag";
    private static final int TEST_FLAG_VALUE = 28;
    private static final String TEST_FLAG_VALUE_STRING = "28";
    private static final int TEST_DEFAULT_FLAG_VALUE = 0;
    private static final int TEST_MAX_FLAG_VALUE = 1000;
    private static final int TEST_MIN_FLAG_VALUE = 100;
    private static final long TEST_PACKAGE_VERSION = 290000000;
    private static final String TEST_PACKAGE_NAME = "test.package.name";
    // The APEX name is the name of the APEX module, as in android.content.pm.ModuleInfo, and is
    // used for its mount point in /apex. APEX packages are actually APKs with a different
    // file extension, so they have an AndroidManifest: the APEX package name is the package name in
    // that manifest, and is reflected in android.content.pm.ApplicationInfo. Contrary to the APEX
    // (module) name, different package names are typically used to identify the organization that
    // built and signed the APEX modules.
    private static final String TEST_APEX_NAME = "com.android.tethering";
    private static final String TEST_APEX_PACKAGE_NAME = "com.prefix.android.tethering";
    private static final String TEST_GO_APEX_PACKAGE_NAME = "com.prefix.android.go.tethering";
    private static final String TEST_CONNRES_PACKAGE_NAME =
            "com.prefix.android.connectivity.resources";
    private final PackageInfo mPackageInfo = new PackageInfo();
    private final PackageInfo mApexPackageInfo = new PackageInfo();
    private MockitoSession mSession;

    @Mock private Context mContext;
    @Mock private PackageManager mPm;
    @Mock private Resources mResources;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mSession = mockitoSession().spyStatic(DeviceConfig.class).startMocking();

        mPackageInfo.setLongVersionCode(TEST_PACKAGE_VERSION);
        mApexPackageInfo.setLongVersionCode(TEST_PACKAGE_VERSION);

        doReturn(mPm).when(mContext).getPackageManager();
        doReturn(TEST_PACKAGE_NAME).when(mContext).getPackageName();
        doThrow(NameNotFoundException.class).when(mPm).getPackageInfo(anyString(), anyInt());
        doReturn(mPackageInfo).when(mPm).getPackageInfo(eq(TEST_PACKAGE_NAME), anyInt());
        doReturn(mApexPackageInfo).when(mPm).getPackageInfo(eq(TEST_APEX_PACKAGE_NAME), anyInt());

        doReturn(mResources).when(mContext).getResources();

        final ResolveInfo ri = new ResolveInfo();
        ri.activityInfo = new ActivityInfo();
        ri.activityInfo.applicationInfo = new ApplicationInfo();
        ri.activityInfo.applicationInfo.packageName = TEST_CONNRES_PACKAGE_NAME;
        ri.activityInfo.applicationInfo.sourceDir =
                "/apex/com.android.tethering/priv-app/ServiceConnectivityResources@version";
        doReturn(Arrays.asList(ri)).when(mPm).queryIntentActivities(argThat(
                intent -> intent.getAction().equals(DeviceConfigUtils.RESOURCES_APK_INTENT)),
                eq(MATCH_SYSTEM_ONLY));
    }

    @After
    public void tearDown() {
        mSession.finishMocking();
        DeviceConfigUtils.resetPackageVersionCacheForTest();
    }

    @Test
    public void testGetDeviceConfigPropertyInt_Null() {
        doReturn(null).when(() -> DeviceConfig.getProperty(eq(TEST_NAME_SPACE),
                eq(TEST_EXPERIMENT_FLAG)));
        assertEquals(TEST_DEFAULT_FLAG_VALUE, DeviceConfigUtils.getDeviceConfigPropertyInt(
                TEST_NAME_SPACE, TEST_EXPERIMENT_FLAG,
                TEST_DEFAULT_FLAG_VALUE /* default value */));
    }

    @Test
    public void testGetDeviceConfigPropertyInt_NotNull() {
        doReturn(TEST_FLAG_VALUE_STRING).when(() -> DeviceConfig.getProperty(eq(TEST_NAME_SPACE),
                eq(TEST_EXPERIMENT_FLAG)));
        assertEquals(TEST_FLAG_VALUE, DeviceConfigUtils.getDeviceConfigPropertyInt(
                TEST_NAME_SPACE, TEST_EXPERIMENT_FLAG,
                TEST_DEFAULT_FLAG_VALUE /* default value */));
    }

    @Test
    public void testGetDeviceConfigPropertyInt_NormalValue() {
        doReturn(TEST_FLAG_VALUE_STRING).when(() -> DeviceConfig.getProperty(eq(TEST_NAME_SPACE),
                eq(TEST_EXPERIMENT_FLAG)));
        assertEquals(TEST_FLAG_VALUE, DeviceConfigUtils.getDeviceConfigPropertyInt(
                TEST_NAME_SPACE, TEST_EXPERIMENT_FLAG, 0 /* minimum value */,
                TEST_MAX_FLAG_VALUE /* maximum value */,
                TEST_DEFAULT_FLAG_VALUE /* default value */));
    }

    @Test
    public void testGetDeviceConfigPropertyInt_NullValue() {
        doReturn(null).when(() -> DeviceConfig.getProperty(
                eq(TEST_NAME_SPACE), eq(TEST_EXPERIMENT_FLAG)));
        assertEquals(TEST_DEFAULT_FLAG_VALUE, DeviceConfigUtils.getDeviceConfigPropertyInt(
                TEST_NAME_SPACE, TEST_EXPERIMENT_FLAG, 0 /* minimum value */,
                TEST_MAX_FLAG_VALUE /* maximum value */,
                TEST_DEFAULT_FLAG_VALUE /* default value */));
    }

    @Test
    public void testGetDeviceConfigPropertyInt_OverMaximumValue() {
        doReturn(Integer.toString(TEST_MAX_FLAG_VALUE + 10)).when(() -> DeviceConfig.getProperty(
                eq(TEST_NAME_SPACE), eq(TEST_EXPERIMENT_FLAG)));
        assertEquals(TEST_DEFAULT_FLAG_VALUE, DeviceConfigUtils.getDeviceConfigPropertyInt(
                TEST_NAME_SPACE, TEST_EXPERIMENT_FLAG, TEST_MIN_FLAG_VALUE /* minimum value */,
                TEST_MAX_FLAG_VALUE /* maximum value */,
                TEST_DEFAULT_FLAG_VALUE /* default value */));
    }

    @Test
    public void testGetDeviceConfigPropertyInt_EqualsMaximumValue() {
        doReturn(Integer.toString(TEST_MAX_FLAG_VALUE)).when(() -> DeviceConfig.getProperty(
                eq(TEST_NAME_SPACE), eq(TEST_EXPERIMENT_FLAG)));
        assertEquals(TEST_MAX_FLAG_VALUE, DeviceConfigUtils.getDeviceConfigPropertyInt(
                TEST_NAME_SPACE, TEST_EXPERIMENT_FLAG, TEST_MIN_FLAG_VALUE /* minimum value */,
                TEST_MAX_FLAG_VALUE /* maximum value */,
                TEST_DEFAULT_FLAG_VALUE /* default value */));
    }

    @Test
    public void testGetDeviceConfigPropertyInt_BelowMinimumValue() {
        doReturn(Integer.toString(TEST_MIN_FLAG_VALUE - 10)).when(() -> DeviceConfig.getProperty(
                eq(TEST_NAME_SPACE), eq(TEST_EXPERIMENT_FLAG)));
        assertEquals(TEST_DEFAULT_FLAG_VALUE, DeviceConfigUtils.getDeviceConfigPropertyInt(
                TEST_NAME_SPACE, TEST_EXPERIMENT_FLAG, TEST_MIN_FLAG_VALUE /* minimum value */,
                TEST_MAX_FLAG_VALUE /* maximum value */,
                TEST_DEFAULT_FLAG_VALUE /* default value */));
    }

    @Test
    public void testGetDeviceConfigPropertyInt_EqualsMinimumValue() {
        doReturn(Integer.toString(TEST_MIN_FLAG_VALUE)).when(() -> DeviceConfig.getProperty(
                eq(TEST_NAME_SPACE), eq(TEST_EXPERIMENT_FLAG)));
        assertEquals(TEST_MIN_FLAG_VALUE, DeviceConfigUtils.getDeviceConfigPropertyInt(
                TEST_NAME_SPACE, TEST_EXPERIMENT_FLAG, TEST_MIN_FLAG_VALUE /* minimum value */,
                TEST_MAX_FLAG_VALUE /* maximum value */,
                TEST_DEFAULT_FLAG_VALUE /* default value */));
    }

    @Test
    public void testGetDeviceConfigPropertyBoolean_Null() {
        doReturn(null).when(() -> DeviceConfig.getProperty(eq(TEST_NAME_SPACE),
                eq(TEST_EXPERIMENT_FLAG)));
        assertFalse(DeviceConfigUtils.getDeviceConfigPropertyBoolean(
                TEST_NAME_SPACE, TEST_EXPERIMENT_FLAG,
                false /* default value */));
    }

    @Test
    public void testGetDeviceConfigPropertyBoolean_NotNull() {
        doReturn("true").when(() -> DeviceConfig.getProperty(eq(TEST_NAME_SPACE),
                eq(TEST_EXPERIMENT_FLAG)));
        assertTrue(DeviceConfigUtils.getDeviceConfigPropertyBoolean(
                TEST_NAME_SPACE, TEST_EXPERIMENT_FLAG,
                false /* default value */));
    }

    @Test
    public void testFeatureIsEnabled() {
        doReturn(TEST_FLAG_VALUE_STRING).when(() -> DeviceConfig.getProperty(eq(TEST_NAME_SPACE),
                eq(TEST_EXPERIMENT_FLAG)));
        assertTrue(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG));
        assertTrue(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG, TEST_APEX_NAME, false /* defaultEnabled */));
    }

    @Test
    public void testFeatureDefaultEnabled() {
        doReturn(null).when(() -> DeviceConfig.getProperty(eq(TEST_NAME_SPACE),
                eq(TEST_EXPERIMENT_FLAG)));
        assertFalse(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG));
        assertFalse(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG, TEST_APEX_NAME, false /* defaultEnabled */));
        assertTrue(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG, TEST_APEX_NAME, true /* defaultEnabled */));
    }

    @Test
    public void testFeatureIsEnabledWithException() throws Exception {
        doThrow(NameNotFoundException.class).when(mPm).getPackageInfo(anyString(), anyInt());
        assertFalse(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG));
        assertFalse(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG, TEST_APEX_NAME, false /* defaultEnabled */));
    }

    @Test
    public void testFeatureIsEnabledOnGo() throws Exception {
        doThrow(NameNotFoundException.class).when(mPm).getPackageInfo(
                eq(TEST_APEX_PACKAGE_NAME), anyInt());
        doReturn(mApexPackageInfo).when(mPm).getPackageInfo(
                eq(TEST_GO_APEX_PACKAGE_NAME), anyInt());
        doReturn("0").when(() -> DeviceConfig.getProperty(
                eq(TEST_NAME_SPACE), eq(TEST_EXPERIMENT_FLAG)));

        assertFalse(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG));
        assertFalse(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG, TEST_APEX_NAME, false /* defaultEnabled */));
        assertTrue(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG, TEST_APEX_NAME, true /* defaultEnabled */));

        doReturn(TEST_FLAG_VALUE_STRING).when(() -> DeviceConfig.getProperty(eq(TEST_NAME_SPACE),
                eq(TEST_EXPERIMENT_FLAG)));
        assertTrue(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG, TEST_APEX_NAME, false /* defaultEnabled */));
    }

    @Test
    public void testFeatureIsEnabledCaching_APK() throws Exception {
        doReturn(TEST_FLAG_VALUE_STRING).when(() -> DeviceConfig.getProperty(eq(TEST_NAME_SPACE),
                eq(TEST_EXPERIMENT_FLAG)));
        assertTrue(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG));
        assertTrue(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG));

        // Package info is only queried once
        verify(mContext, times(1)).getPackageManager();
        verify(mContext, times(1)).getPackageName();
        verify(mPm, times(1)).getPackageInfo(anyString(), anyInt());
    }

    @Test
    public void testFeatureIsEnabledCaching_APEX() throws Exception {
        doReturn(TEST_FLAG_VALUE_STRING).when(() -> DeviceConfig.getProperty(eq(TEST_NAME_SPACE),
                eq(TEST_EXPERIMENT_FLAG)));
        assertTrue(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG, TEST_APEX_NAME, false /* defaultEnabled */));
        assertTrue(DeviceConfigUtils.isFeatureEnabled(mContext, TEST_NAME_SPACE,
                TEST_EXPERIMENT_FLAG, TEST_APEX_NAME, false /* defaultEnabled */));

        // Package info is only queried once
        verify(mPm, times(1)).getPackageInfo(anyString(), anyInt());
        verify(mContext, never()).getPackageName();
    }

    @Test
    public void testGetResBooleanConfig() {
        final int someResId = 1234;
        doReturn(true).when(mResources).getBoolean(someResId);
        assertTrue(DeviceConfigUtils.getResBooleanConfig(mContext, someResId, false));
        doReturn(false).when(mResources).getBoolean(someResId);
        assertFalse(DeviceConfigUtils.getResBooleanConfig(mContext, someResId, false));
        doThrow(new Resources.NotFoundException()).when(mResources).getBoolean(someResId);
        assertFalse(DeviceConfigUtils.getResBooleanConfig(mContext, someResId, false));
    }

    @Test
    public void testGetResIntegerConfig() {
        final int someResId = 1234;
        doReturn(2097).when(mResources).getInteger(someResId);
        assertEquals(2097, DeviceConfigUtils.getResIntegerConfig(mContext, someResId, 2098));
        doThrow(new Resources.NotFoundException()).when(mResources).getInteger(someResId);
        assertEquals(2098, DeviceConfigUtils.getResIntegerConfig(mContext, someResId, 2098));
    }
}
