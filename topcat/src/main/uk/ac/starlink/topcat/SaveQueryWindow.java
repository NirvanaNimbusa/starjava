package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.gui.FilestoreTableSaveDialog;
import uk.ac.starlink.table.gui.SQLWriteDialog;
import uk.ac.starlink.table.gui.SystemTableSaveDialog;
import uk.ac.starlink.table.gui.TableLoadChooser;
import uk.ac.starlink.table.gui.TableSaveChooser;
import uk.ac.starlink.table.gui.TableSaveDialog;

/**
 * Window which allows the user to save one or multiple tables.
 *
 * @author   Mark Taylor
 * @since    15 Jul 2010
 */
public class SaveQueryWindow extends QueryWindow {

    private final TableSaveChooser chooser_;
    private final JTabbedPane tabber_;

    /**
     * Constructor.
     *
     * @param  sto  table output marshaller
     * @param  loadChooser   load chooser, used to initialise state
     *                       (for instance directory)
     * @param  parent   parent component
     */
    public SaveQueryWindow( StarTableOutput sto, TableLoadChooser loadChooser,
                            Component parent ) {
        super( "Save Table(s) or Session", parent, false, true );

        /* Place a progress bar. */
        final JProgressBar progBar = placeProgressBar();

        /* Construct and configure the main table chooser widget. */
        final SaveQueryWindow sw = SaveQueryWindow.this;
        chooser_ = new TableSaveChooser( sto,
                                         new TableSaveDialog[] {
                                             new FilestoreTableSaveDialog(),
                                             new SystemTableSaveDialog(),
                                         } ) {
            public StarTable[] getTables() {
                return sw.getSelectedSavePanel().getTables();
            }
            public void done() {
                super.done();
                sw.dispose();
            }
        };
        chooser_.setProgressBar( progBar );
        chooser_.configureFromLoader( loadChooser );

        /* Set up a tabbed pane to provide for different save options. */
        SavePanel[] savers = new SavePanel[] {
            new CurrentSavePanel( chooser_, sto ),
            new MultiSavePanel( chooser_, sto ),
        };
        tabber_ = new JTabbedPane();
        for ( int is = 0; is < savers.length; is++ ) {
            tabber_.addTab( savers[ is ].getTitle(), savers[ is ] );
        }
        tabber_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                SavePanel saver = getSelectedSavePanel();
                if ( saver != null ) {
                    chooser_.getFormatSelector()
                            .setModel( saver.getFormatBoxModel() );
                }
            }
        } );

        /* Place components. */
        JComponent mainBox = new JPanel( new BorderLayout() );
        getAuxControlPanel().add( mainBox, BorderLayout.CENTER );
        mainBox.add( tabber_, BorderLayout.CENTER );
        JComponent chooserLine = Box.createHorizontalBox();
        chooserLine.add( chooser_ );
        chooserLine.add( Box.createHorizontalGlue() );
        mainBox.add( chooserLine, BorderLayout.SOUTH );
        tabber_.setPreferredSize( new Dimension( 400, 120 ) );

        /* Toolbar buttons. */
        List<Action> saveActList = new ArrayList();
        saveActList.addAll( Arrays.asList( chooser_.getSaveDialogActions() ) );
        saveActList.add( chooser_
                        .createSaveDialogAction( new SQLWriteDialog() ) );
        for ( Action act : saveActList ) {
            getToolBar().add( act );
        }
        getToolBar().addSeparator();

        /* Help button. */
        addHelp( "SaveQueryWindow" );
    }

    public boolean perform() {
        return false;
    }

   /**
    * Returns the save chooser used by this window.
    *
    * @return  save chooser
    */
    public TableSaveChooser getSaveChooser() {
        return chooser_;
    }

    /**
     * Returns the save panel currently displayed in the tabber.
     *
     * @return  current save panel
     */
    private SavePanel getSelectedSavePanel() {
        return (SavePanel) tabber_.getSelectedComponent();
    }
}
