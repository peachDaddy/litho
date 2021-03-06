/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.Px;
import android.support.annotation.StringRes;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;

import com.facebook.infer.annotation.ReturnsOwnership;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.reference.DrawableReference;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.support.annotation.Dimension.DP;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaEdge.VERTICAL;

/**
 * Internal class representing both a {@link ComponentLayout} and a
 * {@link com.facebook.litho.ComponentLayout.ContainerBuilder}.
 */
@ThreadConfined(ThreadConfined.ANY)
class InternalNode implements ComponentLayout, ComponentLayout.ContainerBuilder {

  // Used to check whether or not the framework can use style IDs for
  // paddingStart/paddingEnd due to a bug in some Android devices.
  private static final boolean SUPPORTS_RTL = (SDK_INT >= JELLY_BEAN_MR1);

  // When this flag is set, layoutDirection style was explicitly set on this node.
  private static final long PFLAG_LAYOUT_DIRECTION_IS_SET = 1L << 0;
  // When this flag is set, alignSelf was explicitly set on this node.
  private static final long PFLAG_ALIGN_SELF_IS_SET = 1L << 1;
  // When this flag is set, position type was explicitly set on this node.
  private static final long PFLAG_POSITION_TYPE_IS_SET = 1L << 2;
  // When this flag is set, flex was explicitly set on this node.
  private static final long PFLAG_FLEX_IS_SET = 1L << 3;
  // When this flag is set, flex grow was explicitly set on this node.
  private static final long PFLAG_FLEX_GROW_IS_SET = 1L << 4;
  // When this flag is set, flex shrink was explicitly set on this node.
  private static final long PFLAG_FLEX_SHRINK_IS_SET = 1L << 5;
  // When this flag is set, flex basis was explicitly set on this node.
  private static final long PFLAG_FLEX_BASIS_IS_SET = 1L << 6;
  // When this flag is set, importantForAccessibility was explicitly set on this node.
  private static final long PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET = 1L << 7;
  // When this flag is set, duplicateParentState was explicitly set on this node.
  private static final long PFLAG_DUPLICATE_PARENT_STATE_IS_SET = 1L << 8;
  // When this flag is set, margin was explicitly set on this node.
  private static final long PFLAG_MARGIN_IS_SET = 1L << 9;
  // When this flag is set, padding was explicitly set on this node.
  private static final long PFLAG_PADDING_IS_SET = 1L << 10;
  // When this flag is set, position was explicitly set on this node.
  private static final long PFLAG_POSITION_IS_SET = 1L << 11;
  // When this flag is set, width was explicitly set on this node.
  private static final long PFLAG_WIDTH_IS_SET = 1L << 12;
  // When this flag is set, minWidth was explicitly set on this node.
  private static final long PFLAG_MIN_WIDTH_IS_SET = 1L << 13;
  // When this flag is set, maxWidth was explicitly set on this node.
  private static final long PFLAG_MAX_WIDTH_IS_SET = 1L << 14;
  // When this flag is set, height was explicitly set on this node.
  private static final long PFLAG_HEIGHT_IS_SET = 1L << 15;
  // When this flag is set, minHeight was explicitly set on this node.
  private static final long PFLAG_MIN_HEIGHT_IS_SET = 1L << 16;
  // When this flag is set, maxHeight was explicitly set on this node.
  private static final long PFLAG_MAX_HEIGHT_IS_SET = 1L << 17;
  // When this flag is set, background was explicitly set on this node.
  private static final long PFLAG_BACKGROUND_IS_SET = 1L << 18;
  // When this flag is set, foreground was explicitly set on this node.
  private static final long PFLAG_FOREGROUND_IS_SET = 1L << 19;
  // When this flag is set, visibleHandler was explicitly set on this node.
  private static final long PFLAG_VISIBLE_HANDLER_IS_SET = 1L << 20;
  // When this flag is set, focusedHandler was explicitly set on this node.
  private static final long PFLAG_FOCUSED_HANDLER_IS_SET = 1L << 21;
  // When this flag is set, fullImpressionHandler was explicitly set on this node.
  private static final long PFLAG_FULL_IMPRESSION_HANDLER_IS_SET = 1L << 22;
  // When this flag is set, invisibleHandler was explicitly set on this node.
  private static final long PFLAG_INVISIBLE_HANDLER_IS_SET = 1L << 23;
  // When this flag is set, unfocusedHandler was explicitly set on this node.
  private static final long PFLAG_UNFOCUSED_HANDLER_IS_SET = 1L << 24;
  // When this flag is set, touch expansion was explicitly set on this node.
  private static final long PFLAG_TOUCH_EXPANSION_IS_SET = 1L << 25;
  // When this flag is set, border width was explicitly set on this node.
  private static final long PFLAG_BORDER_WIDTH_IS_SET = 1L << 26;
  // When this flag is set, aspectRatio was explicitly set on this node.
  private static final long PFLAG_ASPECT_RATIO_IS_SET = 1L << 27;
  // When this flag is set, transitionKey was explicitly set on this node.
  private static final long PFLAG_TRANSITION_KEY_IS_SET = 1L << 28;
  // When this flag is set, border color was explicitly set on this node.
  private static final long PFLAG_BORDER_COLOR_IS_SET = 1L << 29;

  private final ResourceResolver mResourceResolver = new ResourceResolver();

  YogaNode mYogaNode;
  private ComponentContext mComponentContext;
  private Resources mResources;
  @ThreadConfined(ThreadConfined.ANY)
  private List<Component> mComponents = new ArrayList(1);
  private int mImportantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
  private boolean mDuplicateParentState;
  private boolean mIsNestedTreeHolder;
  private InternalNode mNestedTree;
  private InternalNode mNestedTreeHolder;
  private long mPrivateFlags;

  private Reference<? extends Drawable> mBackground;
  private Drawable mForeground;
  private int mBorderColor = Color.TRANSPARENT;

  private NodeInfo mNodeInfo;
  private boolean mForceViewWrapping;
  private String mTransitionKey;
  private float mVisibleHeightRatio;
  private float mVisibleWidthRatio;
  private EventHandler<VisibleEvent> mVisibleHandler;
  private EventHandler<FocusedVisibleEvent> mFocusedHandler;
  private EventHandler<UnfocusedVisibleEvent> mUnfocusedHandler;
  private EventHandler<FullImpressionVisibleEvent> mFullImpressionHandler;
  private EventHandler<InvisibleEvent> mInvisibleHandler;
  private String mTestKey;
  private Edges mTouchExpansion;
  private Edges mNestedTreePadding;
  private Edges mNestedTreeBorderWidth;
  private boolean[] mIsPaddingPercent;

  private float mResolvedTouchExpansionLeft = YogaConstants.UNDEFINED;
  private float mResolvedTouchExpansionRight = YogaConstants.UNDEFINED;
  private float mResolvedX = YogaConstants.UNDEFINED;
  private float mResolvedY = YogaConstants.UNDEFINED;
  private float mResolvedWidth = YogaConstants.UNDEFINED;
  private float mResolvedHeight = YogaConstants.UNDEFINED;

  private int mLastWidthSpec = DiffNode.UNSPECIFIED;
  private int mLastHeightSpec = DiffNode.UNSPECIFIED;
  private float mLastMeasuredWidth = DiffNode.UNSPECIFIED;
  private float mLastMeasuredHeight = DiffNode.UNSPECIFIED;
  private DiffNode mDiffNode;

  private boolean mCachedMeasuresValid;
  private TreeProps mPendingTreeProps;

  void init(YogaNode yogaNode, ComponentContext componentContext, Resources resources) {
    yogaNode.setData(this);
    mYogaNode = yogaNode;

    mComponentContext = componentContext;
    mResources = resources;
    mResourceResolver.init(
        mComponentContext,
        componentContext.getResourceCache());
  }

  @Px
  @Override
  public int getX() {
    if (YogaConstants.isUndefined(mResolvedX)) {
      mResolvedX = mYogaNode.getLayoutX();
    }

    return (int) mResolvedX;
  }

  @Px
  @Override
  public int getY() {
    if (YogaConstants.isUndefined(mResolvedY)) {
      mResolvedY = mYogaNode.getLayoutY();
    }

    return (int) mResolvedY;
  }

  @Px
  @Override
  public int getWidth() {
    if (YogaConstants.isUndefined(mResolvedWidth)) {
      mResolvedWidth = mYogaNode.getLayoutWidth();
    }

    return (int) mResolvedWidth;
  }

  @Px
  @Override
  public int getHeight() {
    if (YogaConstants.isUndefined(mResolvedHeight)) {
      mResolvedHeight = mYogaNode.getLayoutHeight();
    }

    return (int) mResolvedHeight;
  }

  @Px
  @Override
  public int getPaddingLeft() {
    return FastMath.round(mYogaNode.getLayoutPadding(LEFT));
  }

  @Px
  @Override
  public int getPaddingTop() {
    return FastMath.round(mYogaNode.getLayoutPadding(TOP));
  }

  @Px
  @Override
  public int getPaddingRight() {
    return FastMath.round(mYogaNode.getLayoutPadding(RIGHT));
  }

  @Px
  @Override
  public int getPaddingBottom() {
    return FastMath.round(mYogaNode.getLayoutPadding(BOTTOM));
  }

  public Reference<? extends Drawable> getBackground() {
    return mBackground;
  }

  public Drawable getForeground() {
    return mForeground;
  }

  public void setCachedMeasuresValid(boolean valid) {
    mCachedMeasuresValid = valid;
  }

  public int getLastWidthSpec() {
    return mLastWidthSpec;
  }

  public void setLastWidthSpec(int widthSpec) {
    mLastWidthSpec = widthSpec;
  }

  public int getLastHeightSpec() {
    return mLastHeightSpec;
  }

  public void setLastHeightSpec(int heightSpec) {
    mLastHeightSpec = heightSpec;
  }

  public boolean hasVisibilityHandlers() {
    return mVisibleHandler != null
        || mFocusedHandler != null
        || mUnfocusedHandler != null
        || mFullImpressionHandler != null
        || mInvisibleHandler != null;
  }

  /**
   * The last value the measure funcion associated with this node {@link Component} returned
   * for the width. This is used together with {@link InternalNode#getLastWidthSpec()}
   * to implement measure caching.
   */
  float getLastMeasuredWidth() {
    return mLastMeasuredWidth;
  }

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the width.
   */
  void setLastMeasuredWidth(float lastMeasuredWidth) {
    mLastMeasuredWidth = lastMeasuredWidth;
  }

  /**
   * The last value the measure funcion associated with this node {@link Component} returned
   * for the height. This is used together with {@link InternalNode#getLastHeightSpec()}
   * to implement measure caching.
   */
  float getLastMeasuredHeight() {
    return mLastMeasuredHeight;
  }

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the height.
   */
  void setLastMeasuredHeight(float lastMeasuredHeight) {
    mLastMeasuredHeight = lastMeasuredHeight;
  }

  DiffNode getDiffNode() {
    return mDiffNode;
  }

  boolean areCachedMeasuresValid() {
    return mCachedMeasuresValid;
  }

  void setDiffNode(DiffNode diffNode) {
    mDiffNode = diffNode;
  }

  /**
   * Mark this node as a nested tree root holder.
   */
  void markIsNestedTreeHolder(TreeProps currentTreeProps) {
    mIsNestedTreeHolder = true;
    mPendingTreeProps = TreeProps.copy(currentTreeProps);
  }

  /**
   * @return Whether this node is holding a nested tree or not. The decision was made during
   * tree creation {@link ComponentLifecycle#createLayout(ComponentContext, Component, boolean)}.
   */
  boolean isNestedTreeHolder() {
    return mIsNestedTreeHolder;
  }

  @Override
  public YogaDirection getResolvedLayoutDirection() {
    return mYogaNode.getLayoutDirection();
  }

  @Override
  public InternalNode layoutDirection(YogaDirection direction) {
    mPrivateFlags |= PFLAG_LAYOUT_DIRECTION_IS_SET;
    mYogaNode.setDirection(direction);
    return this;
  }

  InternalNode flexDirection(YogaFlexDirection direction) {
    mYogaNode.setFlexDirection(direction);
    return this;
  }

  @Override
  public InternalNode wrap(YogaWrap wrap) {
    mYogaNode.setWrap(wrap);
    return this;
  }

  @Override
  public InternalNode justifyContent(YogaJustify justifyContent) {
    mYogaNode.setJustifyContent(justifyContent);
    return this;
  }

  @Override
  public InternalNode alignItems(YogaAlign alignItems) {
    mYogaNode.setAlignItems(alignItems);
    return this;
  }

  @Override
  public InternalNode alignContent(YogaAlign alignContent) {
    mYogaNode.setAlignContent(alignContent);
    return this;
  }

  @Override
  public InternalNode alignSelf(YogaAlign alignSelf) {
    mPrivateFlags |= PFLAG_ALIGN_SELF_IS_SET;
    mYogaNode.setAlignSelf(alignSelf);
    return this;
  }

  @Override
  public InternalNode positionType(YogaPositionType positionType) {
    mPrivateFlags |= PFLAG_POSITION_TYPE_IS_SET;
    mYogaNode.setPositionType(positionType);
    return this;
  }

  @Override
  public InternalNode flex(float flex) {
    mPrivateFlags |= PFLAG_FLEX_IS_SET;
    mYogaNode.setFlex(flex);
    return this;
  }

  @Override
  public InternalNode flexGrow(float flexGrow) {
    mPrivateFlags |= PFLAG_FLEX_GROW_IS_SET;
    mYogaNode.setFlexGrow(flexGrow);
    return this;
  }

  @Override
  public InternalNode flexShrink(float flexShrink) {
    mPrivateFlags |= PFLAG_FLEX_SHRINK_IS_SET;
    mYogaNode.setFlexShrink(flexShrink);
    return this;
  }

  @Override
  public InternalNode flexBasisPx(@Px int flexBasis) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
    mYogaNode.setFlexBasis(flexBasis);
    return this;
  }

  // Used by stetho to re-set auto value
  InternalNode flexBasisAuto() {
    mYogaNode.setFlexBasisAuto();
    return this;
  }

  @Override
  public InternalNode flexBasisPercent(float percent) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
    mYogaNode.setFlexBasisPercent(percent);
    return this;
  }

  @Override
  public InternalNode flexBasisAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return flexBasisPx(mResourceResolver.resolveDimenOffsetAttr(resId, defaultResId));
  }

  @Override
  public InternalNode flexBasisAttr(@AttrRes int resId) {
    return flexBasisAttr(resId, 0);
  }

  @Override
  public InternalNode flexBasisRes(@DimenRes int resId) {
    return flexBasisPx(mResourceResolver.resolveDimenOffsetRes(resId));
  }

  @Override
  public InternalNode flexBasisDip(@Dimension(unit = DP) int flexBasis) {
    return flexBasisPx(mResourceResolver.dipsToPixels(flexBasis));
  }

  @Override
  public InternalNode importantForAccessibility(int importantForAccessibility) {
    mPrivateFlags |= PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET;
    mImportantForAccessibility = importantForAccessibility;
    return this;
  }

  @Override
  public InternalNode duplicateParentState(boolean duplicateParentState) {
    mPrivateFlags |= PFLAG_DUPLICATE_PARENT_STATE_IS_SET;
    mDuplicateParentState = duplicateParentState;
    return this;
  }

  @Override
  public InternalNode marginPx(YogaEdge edge, @Px int margin) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    mYogaNode.setMargin(edge, margin);
    return this;
  }

  @Override
  public InternalNode marginPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    mYogaNode.setMarginPercent(edge, percent);
    return this;
  }

  @Override
  public InternalNode marginAuto(YogaEdge edge) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    mYogaNode.setMarginAuto(edge);
    return this;
  }

  @Override
  public InternalNode marginAttr(
      YogaEdge edge,
      @AttrRes int resId,
      @DimenRes int defaultResId) {
    return marginPx(edge, mResourceResolver.resolveDimenOffsetAttr(resId, defaultResId));
  }

  @Override
  public InternalNode marginAttr(
      YogaEdge edge,
      @AttrRes int resId) {
    return marginAttr(edge, resId, 0);
  }

  @Override
  public InternalNode marginRes(YogaEdge edge, @DimenRes int resId) {
    return marginPx(edge, mResourceResolver.resolveDimenOffsetRes(resId));
  }

  @Override
  public InternalNode marginDip(YogaEdge edge, @Dimension(unit = DP) int margin) {
    return marginPx(edge, mResourceResolver.dipsToPixels(margin));
  }

  @ReturnsOwnership
  private Edges getNestedTreePadding() {
    if (mNestedTreePadding == null) {
      mNestedTreePadding = ComponentsPools.acquireEdges();
    }
    return mNestedTreePadding;
  }

  @Override
  public InternalNode paddingPx(YogaEdge edge, @Px int padding) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;

    if (mIsNestedTreeHolder) {
      getNestedTreePadding().set(edge, padding);
      setIsPaddingPercent(edge, false);
    } else {
      mYogaNode.setPadding(edge, padding);
    }

    return this;
  }

  @Override
  public InternalNode paddingPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;

    if (mIsNestedTreeHolder) {
      getNestedTreePadding().set(edge, percent);
      setIsPaddingPercent(edge, true);
    } else {
      mYogaNode.setPaddingPercent(edge, percent);
    }

    return this;
  }

  @Override
  public InternalNode paddingAttr(
      YogaEdge edge,
      @AttrRes int resId,
      @DimenRes int defaultResId) {
    return paddingPx(edge, mResourceResolver.resolveDimenOffsetAttr(resId, defaultResId));
  }

  @Override
  public InternalNode paddingAttr(
      YogaEdge edge,
      @AttrRes int resId) {
    return paddingAttr(edge, resId, 0);
  }

  @Override
  public InternalNode paddingRes(YogaEdge edge, @DimenRes int resId) {
    return paddingPx(edge, mResourceResolver.resolveDimenOffsetRes(resId));
  }

  @Override
  public InternalNode paddingDip(YogaEdge edge, @Dimension(unit = DP) int padding) {
    return paddingPx(edge, mResourceResolver.dipsToPixels(padding));
  }

  @Override
  public InternalNode borderWidthPx(YogaEdge edge, @Px int borderWidth) {
    mPrivateFlags |= PFLAG_BORDER_WIDTH_IS_SET;

    if (mIsNestedTreeHolder) {
      if (mNestedTreeBorderWidth == null) {
        mNestedTreeBorderWidth = ComponentsPools.acquireEdges();
      }

      mNestedTreeBorderWidth.set(edge, borderWidth);
    } else {
      mYogaNode.setBorder(edge, borderWidth);
    }

    return this;
  }

  @Override
  public InternalNode borderWidthAttr(
      YogaEdge edge,
      @AttrRes int resId,
      @DimenRes int defaultResId) {
    return borderWidthPx(edge, mResourceResolver.resolveDimenOffsetAttr(resId, defaultResId));
  }

  @Override
  public InternalNode borderWidthAttr(
      YogaEdge edge,
      @AttrRes int resId) {
    return borderWidthAttr(edge, resId, 0);
  }

  @Override
  public InternalNode borderWidthRes(YogaEdge edge, @DimenRes int resId) {
    return borderWidthPx(edge, mResourceResolver.resolveDimenOffsetRes(resId));
  }

  @Override
  public InternalNode borderWidthDip(
      YogaEdge edge,
      @Dimension(unit = DP) int borderWidth) {
    return borderWidthPx(edge, mResourceResolver.dipsToPixels(borderWidth));
  }

  @Override
  public Builder borderColor(@ColorInt int borderColor) {
    mPrivateFlags |= PFLAG_BORDER_COLOR_IS_SET;
    mBorderColor = borderColor;
    return this;
  }

  @Override
  public InternalNode positionPx(YogaEdge edge, @Px int position) {
    mPrivateFlags |= PFLAG_POSITION_IS_SET;
    mYogaNode.setPosition(edge, position);
    return this;
  }

  @Override
  public InternalNode positionPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_POSITION_IS_SET;
    mYogaNode.setPositionPercent(edge, percent);
    return this;
  }

  @Override
  public InternalNode positionAttr(
      YogaEdge edge,
      @AttrRes int resId,
      @DimenRes int defaultResId) {
    return positionPx(edge, mResourceResolver.resolveDimenOffsetAttr(resId, defaultResId));
  }

  @Override
  public InternalNode positionAttr(YogaEdge edge, @AttrRes int resId) {
    return positionAttr(edge, resId, 0);
  }

  @Override
  public InternalNode positionRes(YogaEdge edge, @DimenRes int resId) {
    return positionPx(edge, mResourceResolver.resolveDimenOffsetRes(resId));
  }

  @Override
  public InternalNode positionDip(
      YogaEdge edge,
      @Dimension(unit = DP) int position) {
    return positionPx(edge, mResourceResolver.dipsToPixels(position));
  }

  @Override
  public InternalNode widthPx(@Px int width) {
    mPrivateFlags |= PFLAG_WIDTH_IS_SET;
    mYogaNode.setWidth(width);
    return this;
  }

  // Used by stetho to re-set auto value
  InternalNode widthAuto() {
    mYogaNode.setWidthAuto();
    return this;
  }

  @Override
  public InternalNode widthPercent(float percent) {
    mPrivateFlags |= PFLAG_WIDTH_IS_SET;
    mYogaNode.setWidthPercent(percent);
    return this;
  }

  @Override
  public InternalNode widthRes(@DimenRes int resId) {
    return widthPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public InternalNode widthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return widthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public InternalNode widthAttr(@AttrRes int resId) {
    return widthAttr(resId, 0);
  }

  @Override
  public InternalNode widthDip(@Dimension(unit = DP) int width) {
    return widthPx(mResourceResolver.dipsToPixels(width));
  }

  @Override
  public InternalNode minWidthPx(@Px int minWidth) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
    mYogaNode.setMinWidth(minWidth);
    return this;
  }

  @Override
  public InternalNode minWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
    mYogaNode.setMinWidthPercent(percent);
    return this;
  }

  @Override
  public InternalNode minWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return minWidthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public InternalNode minWidthAttr(@AttrRes int resId) {
    return minWidthAttr(resId, 0);
  }

  @Override
  public InternalNode minWidthRes(@DimenRes int resId) {
    return minWidthPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public InternalNode minWidthDip(@Dimension(unit = DP) int minWidth) {
    return minWidthPx(mResourceResolver.dipsToPixels(minWidth));
  }

  @Override
  public InternalNode maxWidthPx(@Px int maxWidth) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
    mYogaNode.setMaxWidth(maxWidth);
    return this;
  }

  @Override
  public InternalNode maxWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
    mYogaNode.setMaxWidthPercent(percent);
    return this;
  }

  @Override
  public InternalNode maxWidthAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return maxWidthPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public InternalNode maxWidthAttr(@AttrRes int resId) {
    return maxWidthAttr(resId, 0);
  }

  @Override
  public InternalNode maxWidthRes(@DimenRes int resId) {
    return maxWidthPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public InternalNode maxWidthDip(@Dimension(unit = DP) int maxWidth) {
    return maxWidthPx(mResourceResolver.dipsToPixels(maxWidth));
  }

  @Override
  public InternalNode heightPx(@Px int height) {
    mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
    mYogaNode.setHeight(height);
    return this;
  }

  // Used by stetho to re-set auto value
  InternalNode heightAuto() {
    mYogaNode.setHeightAuto();
    return this;
  }

  @Override
  public InternalNode heightPercent(float percent) {
    mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
    mYogaNode.setHeightPercent(percent);
    return this;
  }

  @Override
  public InternalNode heightRes(@DimenRes int resId) {
    return heightPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public InternalNode heightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return heightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public InternalNode heightAttr(@AttrRes int resId) {
    return heightAttr(resId, 0);
  }

  @Override
  public InternalNode heightDip(@Dimension(unit = DP) int height) {
    return heightPx(mResourceResolver.dipsToPixels(height));
  }

  @Override
  public InternalNode minHeightPx(@Px int minHeight) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
    mYogaNode.setMinHeight(minHeight);
    return this;
  }

  @Override
  public InternalNode minHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
    mYogaNode.setMinHeightPercent(percent);
    return this;
  }

  @Override
  public InternalNode minHeightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return minHeightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public InternalNode minHeightAttr(@AttrRes int resId) {
    return minHeightAttr(resId, 0);
  }

  @Override
  public InternalNode minHeightRes(@DimenRes int resId) {
    return minHeightPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public InternalNode minHeightDip(@Dimension(unit = DP) int minHeight) {
    return minHeightPx(mResourceResolver.dipsToPixels(minHeight));
  }

  @Override
  public InternalNode maxHeightPx(@Px int maxHeight) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
    mYogaNode.setMaxHeight(maxHeight);
    return this;
  }

  @Override
  public InternalNode maxHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
    mYogaNode.setMaxHeightPercent(percent);
    return this;
  }

  @Override
  public InternalNode maxHeightAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return maxHeightPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public InternalNode maxHeightAttr(@AttrRes int resId) {
    return maxHeightAttr(resId, 0);
  }

  @Override
  public InternalNode maxHeightRes(@DimenRes int resId) {
    return maxHeightPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public InternalNode maxHeightDip(@Dimension(unit = DP) int maxHeight) {
    return maxHeightPx(mResourceResolver.dipsToPixels(maxHeight));
  }

  @Override
  public InternalNode aspectRatio(float aspectRatio) {
    mPrivateFlags |= PFLAG_ASPECT_RATIO_IS_SET;
    mYogaNode.setAspectRatio(aspectRatio);
    return this;
  }

  private boolean shouldApplyTouchExpansion() {
    return mTouchExpansion != null && mNodeInfo != null && mNodeInfo.hasTouchEventHandlers();
  }

  boolean hasTouchExpansion() {
    return ((mPrivateFlags & PFLAG_TOUCH_EXPANSION_IS_SET) != 0L);
  }

  Edges getTouchExpansion() {
    return mTouchExpansion;
  }

  int getTouchExpansionLeft() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    if (YogaConstants.isUndefined(mResolvedTouchExpansionLeft)) {
      mResolvedTouchExpansionLeft = resolveHorizontalEdges(mTouchExpansion, YogaEdge.LEFT);
    }

    return FastMath.round(mResolvedTouchExpansionLeft);
  }

  int getTouchExpansionTop() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(mTouchExpansion.get(YogaEdge.TOP));
  }

  int getTouchExpansionRight() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    if (YogaConstants.isUndefined(mResolvedTouchExpansionRight)) {
      mResolvedTouchExpansionRight = resolveHorizontalEdges(mTouchExpansion, YogaEdge.RIGHT);
    }

    return FastMath.round(mResolvedTouchExpansionRight);
  }

  int getTouchExpansionBottom() {
    if (!shouldApplyTouchExpansion()) {
      return 0;
    }

    return FastMath.round(mTouchExpansion.get(YogaEdge.BOTTOM));
  }

  @Override
  public InternalNode touchExpansionPx(YogaEdge edge, @Px int touchExpansion) {
    if (mTouchExpansion == null) {
      mTouchExpansion = ComponentsPools.acquireEdges();
    }

    mPrivateFlags |= PFLAG_TOUCH_EXPANSION_IS_SET;
    mTouchExpansion.set(edge, touchExpansion);

    return this;
  }

  @Override
  public InternalNode touchExpansionAttr(
      YogaEdge edge,
      @AttrRes int resId,
      @DimenRes int defaultResId) {
    return touchExpansionPx(
        edge,
        mResourceResolver.resolveDimenOffsetAttr(resId, defaultResId));
  }

  @Override
  public InternalNode touchExpansionAttr(
      YogaEdge edge,
      @AttrRes int resId) {
    return touchExpansionAttr(edge, resId, 0);
  }

  @Override
  public InternalNode touchExpansionRes(YogaEdge edge, @DimenRes int resId) {
    return touchExpansionPx(edge, mResourceResolver.resolveDimenOffsetRes(resId));
  }

  @Override
  public InternalNode touchExpansionDip(
      YogaEdge edge,
      @Dimension(unit = DP) int touchExpansion) {
    return touchExpansionPx(edge, mResourceResolver.dipsToPixels(touchExpansion));
  }

  @Override
  public InternalNode child(ComponentLayout child) {
    if (child != null && child != NULL_LAYOUT) {
      addChildAt((InternalNode) child, mYogaNode.getChildCount());
    }
    return this;
  }

  @Override
  public InternalNode child(ComponentLayout.Builder child) {
    if (child != null && child != NULL_LAYOUT) {
      child(child.build());
    }
    return this;
  }

  @Override
  public InternalNode child(Component<?> child) {
    if (child != null) {
      child(Layout.create(mComponentContext, child));
    }
    return this;
  }

  @Override
  public InternalNode child(Component.Builder<?> child) {
    if (child != null) {
      child(child.build());
    }
    return this;
  }

  @Override
  public InternalNode background(Reference<? extends Drawable> background) {
    mPrivateFlags |= PFLAG_BACKGROUND_IS_SET;
    mBackground = background;
    setPaddingFromDrawableReference(background);
    return this;
  }

  @Override
  public InternalNode background(Reference.Builder<? extends Drawable> builder) {
    return background(builder.build());
  }

  @Override
  public InternalNode background(Drawable background) {
    return background(DrawableReference.create().drawable(background));
  }

  @Override
  public InternalNode backgroundAttr(@AttrRes int resId, @DrawableRes int defaultResId) {
    return backgroundRes(mResourceResolver.resolveResIdAttr(resId, defaultResId));
  }

  @Override
  public InternalNode backgroundAttr(@AttrRes int resId) {
    return backgroundAttr(resId, 0);
  }

  @Override
  public InternalNode backgroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return background((Drawable) null);
    }

    return background(mComponentContext.getResources().getDrawable(resId));
  }

  @Override
  public InternalNode backgroundColor(@ColorInt int backgroundColor) {
    return background(new ColorDrawable(backgroundColor));
  }

  @Override
  public InternalNode foreground(Drawable foreground) {
    mPrivateFlags |= PFLAG_FOREGROUND_IS_SET;
    mForeground = foreground;
    return this;
  }

  @Override
  public InternalNode foregroundAttr(@AttrRes int resId, @DrawableRes int defaultResId) {
    return foregroundRes(mResourceResolver.resolveResIdAttr(resId, defaultResId));
  }

  @Override
  public InternalNode foregroundAttr(@AttrRes int resId) {
    return foregroundAttr(resId, 0);
  }

  @Override
  public InternalNode foregroundRes(@DrawableRes int resId) {
    if (resId == 0) {
      return foreground(null);
    }

    return foreground(mResources.getDrawable(resId));
  }

  @Override
  public InternalNode foregroundColor(@ColorInt int foregroundColor) {
    return foreground(new ColorDrawable(foregroundColor));
  }

  @Override
  public InternalNode wrapInView() {
    mForceViewWrapping = true;
    return this;
  }

  boolean isForceViewWrapping() {
    return mForceViewWrapping;
  }

  boolean isClickable() {
    return getOrCreateNodeInfo().getClickHandler() != null;
  }

  @Override
  public InternalNode clickHandler(EventHandler<ClickEvent> clickHandler) {
    getOrCreateNodeInfo().setClickHandler(clickHandler);
    return this;
  }

  @Override
  public InternalNode longClickHandler(EventHandler<LongClickEvent> longClickHandler) {
    getOrCreateNodeInfo().setLongClickHandler(longClickHandler);
    return this;
  }

  @Override
  public InternalNode touchHandler(EventHandler<TouchEvent> touchHandler) {
    getOrCreateNodeInfo().setTouchHandler(touchHandler);
    return this;
  }

  @Override
  public InternalNode interceptTouchHandler(EventHandler interceptTouchHandler) {
    getOrCreateNodeInfo().setInterceptTouchHandler(interceptTouchHandler);
    return this;
  }

  @Override
  public ContainerBuilder focusable(boolean isFocusable) {
    getOrCreateNodeInfo().setFocusable(isFocusable);
    return this;
  }

  @Override
  public ContainerBuilder visibleHeightRatio(float visibleHeightRatio) {
    mVisibleHeightRatio = visibleHeightRatio;
    return this;
  }

  float getVisibleHeightRatio() {
    return mVisibleHeightRatio;
  }

  @Override
  public ContainerBuilder visibleWidthRatio(float visibleWidthRatio) {
    mVisibleWidthRatio = visibleWidthRatio;
    return this;
  }

  float getVisibleWidthRatio() {
    return mVisibleWidthRatio;
  }

  @Override
  public InternalNode visibleHandler(EventHandler<VisibleEvent> visibleHandler) {
    mPrivateFlags |= PFLAG_VISIBLE_HANDLER_IS_SET;
    mVisibleHandler = visibleHandler;
    return this;
  }

  EventHandler<VisibleEvent> getVisibleHandler() {
    return mVisibleHandler;
  }

  @Override
  public InternalNode focusedHandler(EventHandler<FocusedVisibleEvent> focusedHandler) {
    mPrivateFlags |= PFLAG_FOCUSED_HANDLER_IS_SET;
    mFocusedHandler = focusedHandler;
    return this;
  }

  EventHandler<FocusedVisibleEvent> getFocusedHandler() {
    return mFocusedHandler;
  }

  @Override
  public InternalNode unfocusedHandler(EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    mPrivateFlags |= PFLAG_UNFOCUSED_HANDLER_IS_SET;
    mUnfocusedHandler = unfocusedHandler;
    return this;
  }

  EventHandler<UnfocusedVisibleEvent> getUnfocusedHandler() {
    return mUnfocusedHandler;
  }

  @Override
  public InternalNode fullImpressionHandler(
      EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    mPrivateFlags |= PFLAG_FULL_IMPRESSION_HANDLER_IS_SET;
    mFullImpressionHandler = fullImpressionHandler;
    return this;
  }

  EventHandler<FullImpressionVisibleEvent> getFullImpressionHandler() {
    return mFullImpressionHandler;
  }

  @Override
  public InternalNode invisibleHandler(EventHandler<InvisibleEvent> invisibleHandler) {
    mPrivateFlags |= PFLAG_INVISIBLE_HANDLER_IS_SET;
    mInvisibleHandler = invisibleHandler;
    return this;
  }

  EventHandler<InvisibleEvent> getInvisibleHandler() {
    return mInvisibleHandler;
  }

  @Override
  public InternalNode contentDescription(CharSequence contentDescription) {
    getOrCreateNodeInfo().setContentDescription(contentDescription);
    return this;
  }

  @Override
  public InternalNode contentDescription(@StringRes int stringId) {
    return contentDescription(mResources.getString(stringId));
  }

  @Override
  public InternalNode contentDescription(@StringRes int stringId, Object... formatArgs) {
    return contentDescription(mResources.getString(stringId, formatArgs));
  }

  @Override
  public InternalNode viewTag(Object viewTag) {
    getOrCreateNodeInfo().setViewTag(viewTag);
    return this;
  }

  @Override
  public InternalNode viewTags(SparseArray<Object> viewTags) {
    getOrCreateNodeInfo().setViewTags(viewTags);
    return this;
  }

  @Override
  public ContainerBuilder shadowElevationPx(float shadowElevation) {
    getOrCreateNodeInfo().setShadowElevation(shadowElevation);
    return this;
  }

  @Override
  public ContainerBuilder shadowElevationAttr(@AttrRes int resId, @DimenRes int defaultResId) {
    return shadowElevationPx(mResourceResolver.resolveDimenSizeAttr(resId, defaultResId));
  }

  @Override
  public ContainerBuilder shadowElevationAttr(@AttrRes int resId) {
    return shadowElevationAttr(resId, 0);
  }

  @Override
  public ContainerBuilder shadowElevationRes(@DimenRes int resId) {
    return shadowElevationPx(mResourceResolver.resolveDimenSizeRes(resId));
  }

  @Override
  public ContainerBuilder shadowElevationDip(@Dimension(unit = DP) int shadowElevation) {
    return shadowElevationPx(mResourceResolver.dipsToPixels(shadowElevation));
  }

  @Override
  public ContainerBuilder outlineProvider(ViewOutlineProvider outlineProvider) {
    getOrCreateNodeInfo().setOutlineProvider(outlineProvider);
    return this;
  }

  @Override
  public ContainerBuilder clipToOutline(boolean clipToOutline) {
    getOrCreateNodeInfo().setClipToOutline(clipToOutline);
    return this;
  }

  @Override
  public InternalNode testKey(String testKey) {
    mTestKey = testKey;
    return this;
  }

  @Override
  public InternalNode dispatchPopulateAccessibilityEventHandler(
      EventHandler<DispatchPopulateAccessibilityEventEvent>
          dispatchPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo().setDispatchPopulateAccessibilityEventHandler(
        dispatchPopulateAccessibilityEventHandler);
    return this;
  }

  @Override
  public InternalNode onInitializeAccessibilityEventHandler(
      EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler) {
    getOrCreateNodeInfo().setOnInitializeAccessibilityEventHandler(
        onInitializeAccessibilityEventHandler);
    return this;
  }

  @Override
  public InternalNode onInitializeAccessibilityNodeInfoHandler(
      EventHandler<OnInitializeAccessibilityNodeInfoEvent>
          onInitializeAccessibilityNodeInfoHandler) {
    getOrCreateNodeInfo().setOnInitializeAccessibilityNodeInfoHandler(
        onInitializeAccessibilityNodeInfoHandler);
    return this;
  }

  @Override
  public InternalNode onPopulateAccessibilityEventHandler(
      EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
    getOrCreateNodeInfo().setOnPopulateAccessibilityEventHandler(
        onPopulateAccessibilityEventHandler);
    return this;
  }

  @Override
  public InternalNode onRequestSendAccessibilityEventHandler(
      EventHandler<OnRequestSendAccessibilityEventEvent> onRequestSendAccessibilityEventHandler) {
    getOrCreateNodeInfo().setOnRequestSendAccessibilityEventHandler(
        onRequestSendAccessibilityEventHandler);
    return this;
  }

  @Override
  public InternalNode performAccessibilityActionHandler(
      EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
    getOrCreateNodeInfo().setPerformAccessibilityActionHandler(performAccessibilityActionHandler);
    return this;
  }

  @Override
  public InternalNode sendAccessibilityEventHandler(
      EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
    getOrCreateNodeInfo().setSendAccessibilityEventHandler(sendAccessibilityEventHandler);
    return this;
  }

  @Override
  public InternalNode sendAccessibilityEventUncheckedHandler(
      EventHandler<SendAccessibilityEventUncheckedEvent> sendAccessibilityEventUncheckedHandler) {
    getOrCreateNodeInfo().setSendAccessibilityEventUncheckedHandler(
        sendAccessibilityEventUncheckedHandler);
    return this;
  }

  @Override
  public ContainerBuilder transitionKey(String key) {
    if (SDK_INT >= ICE_CREAM_SANDWICH && !TextUtils.isEmpty(key)) {
      mPrivateFlags |= PFLAG_TRANSITION_KEY_IS_SET;
      mTransitionKey = key;
      wrapInView();
    }

    return this;
  }

  String getTransitionKey() {
    return mTransitionKey;
  }

  /**
   * A unique identifier which may be set for retrieving a component and its bounds when testing.
   */
  String getTestKey() {
    return mTestKey;
  }

  void setMeasureFunction(YogaMeasureFunction measureFunction) {
    mYogaNode.setMeasureFunction(measureFunction);
  }

  void setBaselineFunction(YogaBaselineFunction baselineFunction) {
    mYogaNode.setBaselineFunction(baselineFunction);
  }

  boolean hasNewLayout() {
    return mYogaNode.hasNewLayout();
  }

  void markLayoutSeen() {
    mYogaNode.markLayoutSeen();
  }

  float getStyleWidth() {
    return mYogaNode.getWidth().value;
  }

  float getMinWidth() {
    return mYogaNode.getMinWidth().value;
  }

  float getMaxWidth() {
    return mYogaNode.getMaxWidth().value;
  }

  float getStyleHeight() {
    return mYogaNode.getHeight().value;
  }

  float getMinHeight() {
    return mYogaNode.getMinHeight().value;
  }

  float getMaxHeight() {
    return mYogaNode.getMaxHeight().value;
  }

  void calculateLayout(float width, float height) {
    if (ComponentsConfiguration.isDebugModeEnabled) {
      applyOverridesRecursive(this);
    }

    mYogaNode.calculateLayout(width, height);
  }

  private static void applyOverridesRecursive(InternalNode node) {
    DebugComponent.getInstance(node, 0).applyOverrides();
    for (int i = 0, count = node.getChildCount(); i < count; i++) {
      applyOverridesRecursive(node.getChildAt(i));
    }
    if (node.hasNestedTree()) {
      applyOverridesRecursive(node.getNestedTree());
    }
  }

  void calculateLayout() {
    calculateLayout(YogaConstants.UNDEFINED, YogaConstants.UNDEFINED);
  }

  int getChildCount() {
    return mYogaNode.getChildCount();
  }

  com.facebook.yoga.YogaDirection getStyleDirection() {
    return mYogaNode.getStyleDirection();
  }

  InternalNode getChildAt(int index) {
    if (mYogaNode.getChildAt(index) == null) {
      return null;
    }
    return (InternalNode) mYogaNode.getChildAt(index).getData();
  }

  int getChildIndex(InternalNode child) {
    for (int i = 0, count = mYogaNode.getChildCount(); i < count; i++) {
      if (mYogaNode.getChildAt(i) == child.mYogaNode) {
        return i;
      }
    }
    return -1;
  }

  InternalNode getParent() {
    if (mYogaNode == null || mYogaNode.getParent() == null) {
      return null;
    }
    return (InternalNode) mYogaNode.getParent().getData();
  }

  void addChildAt(InternalNode child, int index) {
    mYogaNode.addChildAt(child.mYogaNode, index);
  }

  InternalNode removeChildAt(int index) {
    return (InternalNode) mYogaNode.removeChildAt(index).getData();
  }

  @Override
  public ComponentLayout build() {
    return this;
  }

  private float resolveHorizontalEdges(Edges spacing, YogaEdge edge) {
    final boolean isRtl =
        (mYogaNode.getLayoutDirection() == YogaDirection.RTL);

    final YogaEdge resolvedEdge;
    switch (edge) {
      case LEFT:
        resolvedEdge = (isRtl ? YogaEdge.END : YogaEdge.START);
        break;

      case RIGHT:
        resolvedEdge = (isRtl ? YogaEdge.START : YogaEdge.END);
        break;

      default:
        throw new IllegalArgumentException("Not an horizontal padding edge: " + edge);
    }

    float result = spacing.getRaw(resolvedEdge);
    if (YogaConstants.isUndefined(result)) {
      result = spacing.get(edge);
    }

    return result;
  }

  ComponentContext getContext() {
    return mComponentContext;
  }

  /**
   * Return the list of components contributing to this InternalNode. We have no need for this
   * in production but it is useful information to have while debugging. Therefor this list
   * will only container the root component if running in production mode.
   */
  List<Component> getComponents() {
    return mComponents;
  }

  Component getRootComponent() {
   return mComponents.size() == 0 ? null : mComponents.get(0);
  }

  int getBorderColor() {
    return mBorderColor;
  }

  boolean shouldDrawBorders() {
    return mBorderColor != Color.TRANSPARENT
        && (mYogaNode.getLayoutBorder(LEFT) != 0
            || mYogaNode.getLayoutBorder(TOP) != 0
            || mYogaNode.getLayoutBorder(RIGHT) != 0
            || mYogaNode.getLayoutBorder(BOTTOM) != 0);
  }

  /**
   * Set the root component associated with this internal node. This is the component which created
   * this internal node. If we are in debug mode we also keep track of any delegate components
   * which may have altered anything about this internal node. This is useful when understanding
   * the hierarchy of components in the debugger as well as in stetho.
   */
  void appendComponent(Component component) {
    if (mComponents.size() == 0 || ComponentsConfiguration.isDebugModeEnabled) {
      mComponents.add(component);
    }
  }

  boolean hasNestedTree() {
    return mNestedTree != null;
  }

  @Nullable InternalNode getNestedTree() {
    return mNestedTree;
  }

  InternalNode getNestedTreeHolder() {
    return mNestedTreeHolder;
  }

  /**
   * Set the nested tree before measuring it in order to transfer over important information
   * such as layout direction needed during measurement.
   */
  void setNestedTree(InternalNode nestedTree) {
    nestedTree.mNestedTreeHolder = this;
    mNestedTree = nestedTree;
  }

  NodeInfo getNodeInfo() {
    return mNodeInfo;
  }

  void copyInto(InternalNode node) {
    if (mNodeInfo != null) {
      if (node.mNodeInfo == null) {
        node.mNodeInfo = mNodeInfo.acquireRef();
      } else {
        node.mNodeInfo.updateWith(mNodeInfo);
      }
    }
    if ((node.mPrivateFlags & PFLAG_LAYOUT_DIRECTION_IS_SET) == 0L
        || node.getResolvedLayoutDirection() == YogaDirection.INHERIT) {
      node.layoutDirection(getResolvedLayoutDirection());
    }
    if ((node.mPrivateFlags & PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET) == 0L
        || node.mImportantForAccessibility == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      node.mImportantForAccessibility = mImportantForAccessibility;
    }
    if ((mPrivateFlags & PFLAG_DUPLICATE_PARENT_STATE_IS_SET) != 0L) {
      node.mDuplicateParentState = mDuplicateParentState;
    }
    if ((mPrivateFlags & PFLAG_BACKGROUND_IS_SET) != 0L) {
      node.mBackground = mBackground;
    }
    if ((mPrivateFlags & PFLAG_FOREGROUND_IS_SET) != 0L) {
      node.mForeground = mForeground;
    }
    if (mForceViewWrapping) {
      node.mForceViewWrapping = true;
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_HANDLER_IS_SET) != 0L) {
      node.mVisibleHandler = mVisibleHandler;
    }
    if ((mPrivateFlags & PFLAG_FOCUSED_HANDLER_IS_SET) != 0L) {
      node.mFocusedHandler = mFocusedHandler;
    }
    if ((mPrivateFlags & PFLAG_FULL_IMPRESSION_HANDLER_IS_SET) != 0L) {
      node.mFullImpressionHandler = mFullImpressionHandler;
    }
    if ((mPrivateFlags & PFLAG_INVISIBLE_HANDLER_IS_SET) != 0L) {
      node.mInvisibleHandler = mInvisibleHandler;
    }
    if ((mPrivateFlags & PFLAG_UNFOCUSED_HANDLER_IS_SET) != 0L) {
      node.mUnfocusedHandler = mUnfocusedHandler;
    }
    if (mTestKey != null) {
      node.mTestKey = mTestKey;
    }
    if ((mPrivateFlags & PFLAG_PADDING_IS_SET) != 0L) {
      if (mNestedTreePadding == null) {
        throw new IllegalStateException("copyInto() must be used when resolving a nestedTree. " +
            "If padding was set on the holder node, we must have a mNestedTreePadding instance");
      }

      final YogaNode yogaNode = node.mYogaNode;

      node.mPrivateFlags |= PFLAG_PADDING_IS_SET;
      if (isPaddingPercent(LEFT)) {
        yogaNode.setPaddingPercent(LEFT, mNestedTreePadding.getRaw(YogaEdge.LEFT));
      } else {
        yogaNode.setPadding(LEFT, mNestedTreePadding.getRaw(YogaEdge.LEFT));
      }

      if (isPaddingPercent(TOP)) {
        yogaNode.setPaddingPercent(TOP, mNestedTreePadding.getRaw(YogaEdge.TOP));
      } else {
        yogaNode.setPadding(TOP, mNestedTreePadding.getRaw(YogaEdge.TOP));
      }

      if (isPaddingPercent(RIGHT)) {
        yogaNode.setPaddingPercent(RIGHT, mNestedTreePadding.getRaw(YogaEdge.RIGHT));
      } else {
        yogaNode.setPadding(RIGHT, mNestedTreePadding.getRaw(YogaEdge.RIGHT));
      }

      if (isPaddingPercent(BOTTOM)) {
        yogaNode.setPaddingPercent(BOTTOM, mNestedTreePadding.getRaw(YogaEdge.BOTTOM));
      } else {
        yogaNode.setPadding(BOTTOM, mNestedTreePadding.getRaw(YogaEdge.BOTTOM));
      }

      if (isPaddingPercent(VERTICAL)) {
        yogaNode.setPaddingPercent(VERTICAL, mNestedTreePadding.getRaw(YogaEdge.VERTICAL));
      } else {
        yogaNode.setPadding(VERTICAL, mNestedTreePadding.getRaw(YogaEdge.VERTICAL));
      }

      if (isPaddingPercent(HORIZONTAL)) {
        yogaNode.setPaddingPercent(HORIZONTAL, mNestedTreePadding.getRaw(YogaEdge.HORIZONTAL));
      } else {
        yogaNode.setPadding(HORIZONTAL, mNestedTreePadding.getRaw(YogaEdge.HORIZONTAL));
      }

      if (isPaddingPercent(START)) {
        yogaNode.setPaddingPercent(START, mNestedTreePadding.getRaw(YogaEdge.START));
      } else {
        yogaNode.setPadding(START, mNestedTreePadding.getRaw(YogaEdge.START));
      }

      if (isPaddingPercent(END)) {
        yogaNode.setPaddingPercent(END, mNestedTreePadding.getRaw(YogaEdge.END));
      } else {
        yogaNode.setPadding(END, mNestedTreePadding.getRaw(YogaEdge.END));
      }

      if (isPaddingPercent(ALL)) {
        yogaNode.setPaddingPercent(ALL, mNestedTreePadding.getRaw(YogaEdge.ALL));
      } else {
        yogaNode.setPadding(ALL, mNestedTreePadding.getRaw(YogaEdge.ALL));
      }
    }

    if ((mPrivateFlags & PFLAG_BORDER_WIDTH_IS_SET) != 0L) {
      if (mNestedTreeBorderWidth == null) {
        throw new IllegalStateException("copyInto() must be used when resolving a nestedTree. " +
            "If border width was set on the holder node, we must have a mNestedTreeBorderWidth " +
            "instance");
      }

      final YogaNode yogaNode = node.mYogaNode;

      node.mPrivateFlags |= PFLAG_BORDER_WIDTH_IS_SET;
      yogaNode.setBorder(LEFT, mNestedTreeBorderWidth.getRaw(YogaEdge.LEFT));
      yogaNode.setBorder(TOP, mNestedTreeBorderWidth.getRaw(YogaEdge.TOP));
      yogaNode.setBorder(RIGHT, mNestedTreeBorderWidth.getRaw(YogaEdge.RIGHT));
      yogaNode.setBorder(BOTTOM, mNestedTreeBorderWidth.getRaw(YogaEdge.BOTTOM));
      yogaNode.setBorder(VERTICAL, mNestedTreeBorderWidth.getRaw(YogaEdge.VERTICAL));
      yogaNode.setBorder(HORIZONTAL, mNestedTreeBorderWidth.getRaw(YogaEdge.HORIZONTAL));
      yogaNode.setBorder(START, mNestedTreeBorderWidth.getRaw(YogaEdge.START));
      yogaNode.setBorder(END, mNestedTreeBorderWidth.getRaw(YogaEdge.END));
      yogaNode.setBorder(ALL, mNestedTreeBorderWidth.getRaw(YogaEdge.ALL));
    }
    if ((mPrivateFlags & PFLAG_TRANSITION_KEY_IS_SET) != 0L) {
      node.mTransitionKey = mTransitionKey;
    }
    if ((mPrivateFlags & PFLAG_BORDER_COLOR_IS_SET) != 0L) {
      node.mBorderColor = mBorderColor;
    }
    if (mVisibleHeightRatio != 0) {
      node.mVisibleHeightRatio = mVisibleHeightRatio;
    }
    if (mVisibleWidthRatio != 0) {
      node.mVisibleWidthRatio = mVisibleWidthRatio;
    }
  }

  void setStyleWidthFromSpec(int widthSpec) {
    switch (SizeSpec.getMode(widthSpec)) {
      case SizeSpec.UNSPECIFIED:
        mYogaNode.setWidth(YogaConstants.UNDEFINED);
        break;
      case SizeSpec.AT_MOST:
        mYogaNode.setMaxWidth(SizeSpec.getSize(widthSpec));
        break;
      case SizeSpec.EXACTLY:
        mYogaNode.setWidth(SizeSpec.getSize(widthSpec));
        break;
    }
  }

  void setStyleHeightFromSpec(int heightSpec) {
    switch (SizeSpec.getMode(heightSpec)) {
      case SizeSpec.UNSPECIFIED:
        mYogaNode.setHeight(YogaConstants.UNDEFINED);
        break;
      case SizeSpec.AT_MOST:
        mYogaNode.setMaxHeight(SizeSpec.getSize(heightSpec));
        break;
      case SizeSpec.EXACTLY:
        mYogaNode.setHeight(SizeSpec.getSize(heightSpec));
        break;
    }
  }

  int getImportantForAccessibility() {
    return mImportantForAccessibility;
  }

  boolean isDuplicateParentStateEnabled() {
    return mDuplicateParentState;
  }

  void applyAttributes(TypedArray a) {
    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == R.styleable.ComponentLayout_android_layout_width) {
        int width = a.getLayoutDimension(attr, -1);
        // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
        if (width >= 0) {
          widthPx(width);
        }
      } else if (attr == R.styleable.ComponentLayout_android_layout_height) {
        int height = a.getLayoutDimension(attr, -1);
        // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
        if (height >= 0) {
          heightPx(height);
        }
      } else if (attr == R.styleable.ComponentLayout_android_paddingLeft) {
        paddingPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingTop) {
        paddingPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingRight) {
        paddingPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingBottom) {
        paddingPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingStart && SUPPORTS_RTL) {
        paddingPx(START, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_paddingEnd && SUPPORTS_RTL) {
        paddingPx(END, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_padding) {
        paddingPx(ALL, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginLeft) {
        marginPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginTop) {
        marginPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginRight) {
        marginPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginBottom) {
        marginPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginStart && SUPPORTS_RTL) {
        marginPx(START, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_marginEnd && SUPPORTS_RTL) {
        marginPx(END, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_layout_margin) {
        marginPx(ALL, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_importantForAccessibility &&
          SDK_INT >= JELLY_BEAN) {
        importantForAccessibility(a.getInt(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_android_duplicateParentState) {
        duplicateParentState(a.getBoolean(attr, false));
      } else if (attr == R.styleable.ComponentLayout_android_background) {
        if (TypedArrayUtils.isColorAttribute(a, R.styleable.ComponentLayout_android_background)) {
          backgroundColor(a.getColor(attr, 0));
        } else {
          backgroundRes(a.getResourceId(attr, -1));
        }
      } else if (attr == R.styleable.ComponentLayout_android_foreground) {
        if (TypedArrayUtils.isColorAttribute(a, R.styleable.ComponentLayout_android_foreground)) {
          foregroundColor(a.getColor(attr, 0));
        } else {
          foregroundRes(a.getResourceId(attr, -1));
        }
      } else if (attr == R.styleable.ComponentLayout_android_contentDescription) {
        contentDescription(a.getString(attr));
      } else if (attr == R.styleable.ComponentLayout_flex_direction) {
        flexDirection(YogaFlexDirection.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_wrap) {
        wrap(YogaWrap.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_justifyContent) {
        justifyContent(YogaJustify.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_alignItems) {
        alignItems(YogaAlign.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_alignSelf) {
        alignSelf(YogaAlign.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex_positionType) {
        positionType(YogaPositionType.fromInt(a.getInteger(attr, 0)));
      } else if (attr == R.styleable.ComponentLayout_flex) {
        final float flex = a.getFloat(attr, -1);
        if (flex >= 0f) {
          flex(flex);
        }
      } else if (attr == R.styleable.ComponentLayout_flex_left) {
        positionPx(LEFT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_top) {
        positionPx(TOP, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_right) {
        positionPx(RIGHT, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_bottom) {
        positionPx(BOTTOM, a.getDimensionPixelOffset(attr, 0));
      } else if (attr == R.styleable.ComponentLayout_flex_layoutDirection) {
        final int layoutDirection = a.getInteger(attr, -1);
        layoutDirection(YogaDirection.fromInt(layoutDirection));
      }
    }
  }

  /**
   * Reset all attributes to default values. Intended to facilitate recycling.
   */
  void release() {
    if (mYogaNode.getParent() != null || mYogaNode.getChildCount() > 0) {
      throw new IllegalStateException("You should not free an attached Internalnode");
    }

    ComponentsPools.release(mYogaNode);
    mYogaNode = null;

    mResourceResolver.internalRelease();

    mResolvedTouchExpansionLeft = YogaConstants.UNDEFINED;
    mResolvedTouchExpansionRight = YogaConstants.UNDEFINED;
    mResolvedX = YogaConstants.UNDEFINED;
    mResolvedY = YogaConstants.UNDEFINED;
    mResolvedWidth = YogaConstants.UNDEFINED;
    mResolvedHeight = YogaConstants.UNDEFINED;

    mComponentContext = null;
    mResources = null;
    mComponents.clear();
    mNestedTree = null;
    mNestedTreeHolder = null;

    if (mNodeInfo != null) {
      mNodeInfo.release();
      mNodeInfo = null;
    }
    mImportantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
    mDuplicateParentState = false;
    mBackground = null;
    mForeground = null;
    mForceViewWrapping = false;
    mVisibleHeightRatio = 0;
    mVisibleWidthRatio = 0;
    mVisibleHandler = null;
    mFocusedHandler = null;
    mUnfocusedHandler = null;
    mFullImpressionHandler = null;
    mInvisibleHandler = null;
    mPrivateFlags = 0L;
    mTransitionKey = null;
    mBorderColor = Color.TRANSPARENT;
    mIsPaddingPercent = null;

    if (mTouchExpansion != null) {
      ComponentsPools.release(mTouchExpansion);
      mTouchExpansion = null;
    }
    if (mNestedTreePadding != null) {
      ComponentsPools.release(mNestedTreePadding);
      mNestedTreePadding = null;
    }
    if (mNestedTreeBorderWidth != null) {
      ComponentsPools.release(mNestedTreeBorderWidth);
      mNestedTreeBorderWidth = null;
    }

    mLastWidthSpec = DiffNode.UNSPECIFIED;
    mLastHeightSpec = DiffNode.UNSPECIFIED;
    mLastMeasuredHeight = DiffNode.UNSPECIFIED;
    mLastMeasuredWidth = DiffNode.UNSPECIFIED;
    mDiffNode = null;
    mCachedMeasuresValid = false;
    mIsNestedTreeHolder = false;
    mTestKey = null;

    if (mPendingTreeProps != null) {
      mPendingTreeProps.reset();
      ComponentsPools.release(mPendingTreeProps);
      mPendingTreeProps = null;
    }
  }

  private NodeInfo getOrCreateNodeInfo() {
    if (mNodeInfo == null) {
      mNodeInfo = NodeInfo.acquire();
    }

    return mNodeInfo;
  }

  /**
   * Check that the root of the nested tree we are going to use, has valid layout directions
   * with its main tree holder node.
   */
  static boolean hasValidLayoutDirectionInNestedTree(
      InternalNode nestedTreeHolder,
      InternalNode nestedTree) {
    final boolean nestedTreeHasExplicitDirection =
        ((nestedTree.mPrivateFlags & PFLAG_LAYOUT_DIRECTION_IS_SET) != 0L);
    final boolean hasSameLayoutDirection =
        (nestedTree.getResolvedLayoutDirection() == nestedTreeHolder.getResolvedLayoutDirection());

    return nestedTreeHasExplicitDirection || hasSameLayoutDirection;
  }

  /**
   * Adds an item to a possibly nulled list to defer the allocation as long as possible.
   */
  private static <A> List<A> addOrCreateList(@Nullable List<A> list, A item) {
    if (list == null) {
      list = new LinkedList<>();
    }

    list.add(item);

    return list;
  }

  private void setIsPaddingPercent(YogaEdge edge, boolean isPaddingPercent) {
    if (mIsPaddingPercent == null && isPaddingPercent) {
      mIsPaddingPercent = new boolean[YogaEdge.ALL.intValue() + 1];
    }
    if (mIsPaddingPercent != null) {
      mIsPaddingPercent[edge.intValue()] = isPaddingPercent;
    }
  }

  private boolean isPaddingPercent(YogaEdge edge) {
    return (mIsPaddingPercent == null) ? false : mIsPaddingPercent[edge.intValue()];
  }

  /**
   * Crash if the given node has context specific style set.
   */
  static void assertContextSpecificStyleNotSet(InternalNode node) {
    List<CharSequence> errorTypes = null;

    if ((node.mPrivateFlags & PFLAG_ALIGN_SELF_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "alignSelf");
    }
    if ((node.mPrivateFlags & PFLAG_POSITION_TYPE_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "positionType");
    }
    if ((node.mPrivateFlags & PFLAG_FLEX_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "flex");
    }
    if ((node.mPrivateFlags & PFLAG_FLEX_GROW_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "flexGrow");
    }
    if ((node.mPrivateFlags & PFLAG_MARGIN_IS_SET) != 0L) {
      errorTypes = addOrCreateList(errorTypes, "margin");
    }

    if (errorTypes != null) {
      final CharSequence errorStr = TextUtils.join(", ", errorTypes);
      throw new IllegalStateException("You should not set " + errorStr + " to a root layout in "
          + node.getRootComponent().getLifecycle());
    }
  }

  public TreeProps getPendingTreeProps() {
    return mPendingTreeProps;
  }

  private <T extends Drawable> void setPaddingFromDrawableReference(Reference<T> ref) {
    if (ref == null) {
      return;
    }
    final T drawable = Reference.acquire(mComponentContext,ref);
    if (drawable != null) {
      final Rect backgroundPadding = ComponentsPools.acquireRect();
      if (drawable.getPadding(backgroundPadding)) {
        paddingPx(LEFT, backgroundPadding.left);
        paddingPx(TOP, backgroundPadding.top);
        paddingPx(RIGHT, backgroundPadding.right);
        paddingPx(BOTTOM, backgroundPadding.bottom);
      }

      Reference.release(mComponentContext, drawable, ref);
      ComponentsPools.release(backgroundPadding);
    }
  }
}
