package org.matrix.chromext.proxy

import android.content.Context
import chromium.AppMenuHandler
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.matrix.chromext.Chrome
import org.matrix.chromext.utils.findField
import org.matrix.chromext.utils.reflect.toMethod
import java.util.function.Supplier

@OptIn(DexKitExperimentalApi::class)
object PageMenuProxy {

  val chromeTabbedActivity = UserScriptProxy.chromeTabbedActivity
  val customTabActivity = Chrome.load("org.chromium.chrome.browser.customtabs.CustomTabActivity")
  val class_PropertyModel = Chrome.load("org.chromium.ui.modelutil.PropertyModel")
  val tab = Chrome.load("org.chromium.chrome.browser.tab.Tab")
  val emptyTabObserver =
    Chrome.load("org.chromium.chrome.browser.login.ChromeHttpAuthHandler").superclass as Class<*>
  val tabImpl = UserScriptProxy.tabImpl
  val mIsLoading = UserScriptProxy.mIsLoading
  val mObservers = findField(tabImpl) { type.interfaces.contains(Iterable::class.java) }

  /**
   * package org.chromium.chrome.browser.app.appmenu;
   * public class AppMenuItemUtils {
   *     public static PropertyModel buildModelForStandardMenuItem(
   *             Context context,
   *             AppMenuItemTheme theme,
   *             @IdRes int id,
   *             @StringRes int titleId,
   *             @DrawableRes int iconResId,
   *             boolean isMenuIconAtStart);
   * }
   */
  val method_AppMenuItemUtils_buildModelForStandardMenuItem = Chrome.dexKitBridge.getMethod {
    matcher {
      returnType(class_PropertyModel)
      paramTypes(
        Context::class.java,
        null,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        Boolean::class.javaPrimitiveType
      )
    }
  }.toMethod()

  /**
   * package org.chromium.chrome.browser.app.appmenu;
   * public class AppMenuItemUtils {
   *     public static PropertyModel buildModelForMenuItemWithSubmenu(
   *             Context context,
   *             AppMenuItemTheme theme,
   *             @IdRes int id,
   *             @StringRes int titleId,
   *             @DrawableRes int iconResId,
   *             Supplier<List<ListItem>> submenuItemProvider,
   *             boolean isMenuIconAtStart);
   * }
   */
  val method_AppMenuItemUtils_buildModelForMenuItemWithSubmenu =
    Chrome.dexKitBridge.getMethod {
      matcher {
        returnType(class_PropertyModel)
        paramTypes(
          Context::class.java,
          null,
          Int::class.javaPrimitiveType,
          Int::class.javaPrimitiveType,
          Int::class.javaPrimitiveType,
          Supplier::class.java,
          Boolean::class.javaPrimitiveType
        )
      }
    }.toMethod()

  /**
   * public static MVCListAdapter.ListItem createMenuItemWithSubmenuListItem(
   *             PropertyModel model, boolean showIcon)
   */
  val method_AppMenuItemUtils_createMenuItemWithSubmenuListItem =
    Chrome.dexKitBridge.getMethod {
      matcher {
        declaredClass(method_AppMenuItemUtils_buildModelForStandardMenuItem.declaringClass)
        paramTypes(
          class_PropertyModel, Boolean::class.javaPrimitiveType
        )
        usingNumbers(
          AppMenuHandler.AppMenuItemType.MENU_ITEM_WITH_SUBMENU,
          AppMenuHandler.AppMenuItemType.MENU_ITEM_WITH_SUBMENU_NO_ICON
        )
      }
    }.toMethod()

  /**
   * public static MVCListAdapter.ListItem createStandardListItem(
   *             PropertyModel model, boolean showIcon)
   */
  val method_AppMenuItemUtils_createStandardListItem =
    Chrome.dexKitBridge.getMethod {
      matcher {
        declaredClass(method_AppMenuItemUtils_buildModelForStandardMenuItem.declaringClass)
        paramTypes(
          class_PropertyModel, Boolean::class.javaPrimitiveType
        )
        not {
          name(method_AppMenuItemUtils_createMenuItemWithSubmenuListItem.name)
        }
      }
    }.toMethod()
}
