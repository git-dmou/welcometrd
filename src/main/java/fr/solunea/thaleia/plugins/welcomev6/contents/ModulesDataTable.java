package fr.solunea.thaleia.plugins.welcomev6.contents;

import fr.solunea.thaleia.webapp.utils.NoRecordsToolbarVisibleEmpty;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;

import java.util.List;

@SuppressWarnings("serial")
public class ModulesDataTable<T, S> extends DataTable<T, S> {

    protected ModulesDataTable(String id, List<? extends IColumn<T, S>> columns, ISortableDataProvider<T, S>
            dataProvider, int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);

        // Pagination du tableau
        addTopToolbar(new NavigationToolbar(this));

        addTopToolbar(new HeadersToolbar<>(this, dataProvider));

        addBottomToolbar(new NoRecordsToolbarVisibleEmpty(this));
    }

    @Override
    protected Item<T> newRowItem(final String id, final int index, final IModel<T> model) {
        return new OddEvenItem<>(id, index, model);
    }







}
