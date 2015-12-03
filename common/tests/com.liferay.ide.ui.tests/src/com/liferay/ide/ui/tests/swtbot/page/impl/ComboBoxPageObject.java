
package com.liferay.ide.ui.tests.swtbot.page.impl;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;

import com.liferay.ide.ui.tests.swtbot.condition.WidgetEnabledCondition;
import com.liferay.ide.ui.tests.swtbot.page.IComboBoxPageObject;

public class ComboBoxPageObject<T extends SWTBot> extends AbstractWidgetPageObject<SWTBot>
    implements IComboBoxPageObject
{

    public ComboBoxPageObject( SWTBot bot, String label )
    {
        super( bot, label );
    }

    @Override
    public void setSelection( String value )
    {
        AbstractSWTBot<? extends Widget> widget = getWidget();

        if( widget instanceof SWTBotCombo )
        {
            SWTBotCombo swtBotCombo = (SWTBotCombo) widget;

            bot.waitUntil( new WidgetEnabledCondition( swtBotCombo, true ) );

            swtBotCombo.setSelection( value );
        }
    }

    @Override
    protected AbstractSWTBot<?> getWidget()
    {
        return bot.comboBoxWithLabel( label );
    }

}
