package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Displays the parameters of a registry query and its results.
 * The URL of a registry and the text of a query are displayed at the
 * top of the window, with query submit and cancel buttons.
 * When the submit button is pushed, the specified query is performed 
 * asynchronously on the selected registry.
 *
 * <p>Subclasses can be notified of the completion of a successful query 
 * by overriding the {@link #gotData} method.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class RegistryPanel extends JPanel {

    private final Action submitQueryAction_;
    private final Action cancelQueryAction_;
    private final JScrollPane resScroller_;
    private final RegistryTable regTable_;
    private final JTable capTable_;
    private final CapabilityTableModel capTableModel_;
    private final RegistryQueryFactory queryFactory_;
    private Thread queryWorker_;
    private JComponent workingPanel_;
    private JComponent dataPanel_;
    private List activeItems_;
    private String workingMessage_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a RegistryPanel.
     *
     * @param  queryFactory  object which supplies the query details;
     *         if it has a graphical component, that will be displayed 
     *         for the user to interact with
     * @param  showCapabilities  true to display a selectable table of 
     *         {@link RegCapabilityInterface}s below the table of
     *         {@link RegResource}s
     */
    public RegistryPanel( RegistryQueryFactory queryFactory,
                          boolean showCapabilities ) {
        super( new BorderLayout() );
        queryFactory_ = queryFactory;
        activeItems_ = new ArrayList();

        /* Define actions for submit/cancel registry query. */
        cancelQueryAction_ = new AbstractAction( "Cancel Query" ) {
            public void actionPerformed( ActionEvent evt ) {
                cancelQuery();
            }
        };
        submitQueryAction_ = new AbstractAction( "Submit Query" ) {
            public void actionPerformed( ActionEvent evt ) {
                workingMessage_ = "Searching registry";
                submitQuery();
            }
        };
        activeItems_.add( submitQueryAction_ );
        queryFactory_.addEntryListener( submitQueryAction_ );

        /* Create the component which will hold the query parameters. */
        JComponent queryComponent = queryFactory.getComponent();
        if ( queryComponent != null ) {
            JComponent qBox = Box.createVerticalBox();
            activeItems_.add( queryComponent );
            qBox.add( queryComponent );
            qBox.add( Box.createVerticalStrut( 5 ) );

            /* Component to hold submit/cancel buttons. */
            JComponent controlLine = Box.createHorizontalBox();
            controlLine.add( Box.createHorizontalGlue() );
            controlLine.add( new JButton( cancelQueryAction_ ) );
            controlLine.add( Box.createHorizontalStrut( 5 ) );
            controlLine.add( new JButton( submitQueryAction_ ) );
            qBox.add( controlLine );
            qBox.add( Box.createVerticalStrut( 5 ) );
            add( qBox, BorderLayout.NORTH );
        }
        
        /* Scroll pane which will hold the main data component. 
         * At any point this will hold either workingPanel_ or dataPanel_,
         * according to whether a query is currently in progress. */
        resScroller_ = new JScrollPane();

        /* Create the working panel (it will be populated when shown). */
        workingPanel_ = new JPanel( new BorderLayout() );

        /* Create the table for display of query results. */
        regTable_ = new RegistryTable();
        regTable_.setColumnSelectionAllowed( false );
        regTable_.setRowSelectionAllowed( true );
        dataPanel_ = regTable_;
        setWorking( null );

        /* Create the table for display of per-resource capabilities.
         * This may not be used (if showCapabilities is false), but
         * construct it anyway and just don't place it; this simplifies
         * code using this class so that it doesn't have to change 
         * according to whether showCapabilities is true or not. */
        capTableModel_ = new CapabilityTableModel();
        capTable_ = new JTable( capTableModel_ );
        capTable_.setColumnSelectionAllowed( false );
        capTable_.setRowSelectionAllowed( true );
        final ListSelectionModel regSelModel =
            regTable_.getSelectionModel();
        regSelModel.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                RegResource[] resources = getSelectedResources();
                if ( resources.length == 1 ) {
                    RegCapabilityInterface[] caps =
                        getCapabilities( resources[ 0 ] );
                    capTableModel_.setCapabilities( caps );
                    if ( caps.length > 0 ) {
                        capTable_.setRowSelectionInterval( 0, 0 );
                    }
                }
            }
        } );

        /* Place components. */
        if ( showCapabilities ) {
            JSplitPane splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
            splitter.setTopComponent( resScroller_ );
            splitter.setBottomComponent( new JScrollPane( capTable_ ) );
            splitter.setResizeWeight( 0.8 );
            add( splitter, BorderLayout.CENTER );
        }
        else {
            resScroller_.setBorder( BorderFactory.createEtchedBorder() );
            add( resScroller_, BorderLayout.CENTER );
        }
    }

    /**
     * Invoking this method withdraws the parts of the GUI which permit the
     * user to specify a registry query, and peforms a fixed query without
     * further ado.  This effect cannot be reversed.
     *
     * @param  workingMsg  message to display near progress bar while 
     *         query is ongoing
     */
    public void performAutoQuery( String workingMsg ) {
        workingMessage_ = workingMsg;
        submitQuery();
    }

    /**
     * Called from the event dispatch thread when a successful 
     * registry query which returns 1 or more records has been completed.
     * The default implementation does nothing.
     *
     * @param  resources   non-empty array of resources returned from a
     *                     successful query
     */
    protected void gotData( RegResource[] resources ) {
    }

    /**
     * Returns an array of all the results from the most recently completed
     * registry query.
     *
     * @return   list of query results
     */
    public RegResource[] getResources() {
        return regTable_.getData();
    }

    /**
     * Returns an array of all the relevant capabilities of a given resource.
     *
     * @param  resource
     * @return   capability list
     */
    public RegCapabilityInterface[] getCapabilities( RegResource resource ) {
        return resource.getCapabilities();
    }

    /**
     * Returns an array of any of the results from the most recent 
     * registry query which are currently selected by the user.
     *
     * @return   list of any selected query results
     */
    public RegResource[] getSelectedResources() {
        ListSelectionModel smodel = getResourceSelectionModel();
        List sres = new ArrayList();
        RegResource[] data = getResources();
        for ( int i = smodel.getMinSelectionIndex();
              i <= smodel.getMaxSelectionIndex(); i++ ) {
            if ( smodel.isSelectedIndex( i ) ) {
                sres.add( data[ i ] );
            }
        }
        return (RegResource[]) sres.toArray( new RegResource[ 0 ] );
    }

    /**
     * Returns an array of all the capabilities associated with the 
     * currently selected resource which are themselves currently selected.
     * In the case that there is no capabilities table displayed, it's
     * assumed that all capabilities of the selected resource are selected.
     *
     * @return   capability list
     */
    public RegCapabilityInterface[] getSelectedCapabilities() {
        if ( capTable_ == null ) {
            RegResource[] resources = getSelectedResources();
            List capList = new ArrayList();
            if ( resources != null ) {
                for ( int ir = 0; ir < resources.length; ir++ ) {
                    RegCapabilityInterface[] caps =
                        getCapabilities( resources[ ir ] );
                    if ( caps != null ) {
                        capList.addAll( Arrays.asList( caps ) );
                    }
                }
            }
            return (RegCapabilityInterface[])
                   capList.toArray( new RegCapabilityInterface[ 0 ] );
        }
        else {
            ListSelectionModel smodel = capTable_.getSelectionModel();
            RegCapabilityInterface[] allCaps = capTableModel_.getCapabilities();
            List capList = new ArrayList();
            for ( int i = smodel.getMinSelectionIndex();
                  i <= smodel.getMaxSelectionIndex(); i++ ) {
                if ( smodel.isSelectedIndex( i ) ) {
                    capList.add( allCaps[ i ] );
                }
            }
            return (RegCapabilityInterface[])
                   capList.toArray( new RegCapabilityInterface[ 0 ] );
        }
    }

    /**
     * Invoked when the Submit button is pressed.
     * Performs an asynchronous query on the registry.
     */
    public void submitQuery() {
        
        /* Get the query specification object. */
        final RegistryQuery query;
        try {
            query = queryFactory_.getQuery();
        }
        catch ( Exception e ) {
            ErrorDialog.showError( this, "Query Error", e ); 
            return;
        }
        if ( query == null || query.getText() == null
                           || query.getText().trim() == null ) {
            JOptionPane.showMessageDialog( this, "No query selected",
                                           "No Query",
                                           JOptionPane.ERROR_MESSAGE );
            return;
        }

        /* Begin an asynchronous query on the registry. */
        final JProgressBar progBar = setWorking( workingMessage_ );
        progBar.setStringPainted( true );
        progBar.setString( "Found 0" );
        Thread worker = new Thread( "Registry query" ) {
            List resourceList = new ArrayList();
            String errmsg;
            Thread wk = this;
            public void run() {
                Throwable error = null;
                try {
                    for ( Iterator it = query.getQueryIterator();
                          it.hasNext() && ! isInterrupted() ; ) {
                        resourceList.add( (RegResource) it.next() );
                        progBar.setString( "Found " + resourceList.size() );
                    }
                    logger_.info( "Records found: " + resourceList.size() );
                    if ( resourceList.isEmpty() ) {
                        errmsg = "No resources found for query";
                    }
                }
                catch ( RegistryQueryException e ) {
                    error = e.getCause() == null ? e : e.getCause();
                }
                catch ( Throwable e ) {
                    error = e;
                }
                final Throwable error1 = error;
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( queryWorker_ == wk ) {
                            if ( errmsg != null ) {
                                JOptionPane
                               .showMessageDialog( RegistryPanel.this, errmsg,
                                                   "Query Failed",
                                                   JOptionPane.ERROR_MESSAGE );
                            }
                            else if ( error1 != null ) {
                                ErrorDialog.showError( RegistryPanel.this,
                                                       "Query Error", error1 );
                            }
                            else {
                                RegResource[] resources =
                                    (RegResource[])
                                    resourceList
                                   .toArray( new RegResource[ 0 ] );
                                regTable_.setData( resources );
                                gotData( resources );
                            }
                            setWorking( null );
                        }
                    }
                } );
            }
        };
        Thread oldWorker = queryWorker_;
        if ( oldWorker != null ) {
            oldWorker.interrupt();
        }
        queryWorker_ = worker;
        worker.start();
    }

    /**
     * Invoked when the cancel button is pressed.
     * Deactivates the current query.
     */
    public void cancelQuery() {
        if ( queryWorker_ != null ) {
            queryWorker_.interrupt();
            queryWorker_ = null;
        }
        setWorking( null );
    }

    /**
     * Returns the selection model used by the user to select resource items
     * from a completed query.
     *
     * @return   selection model (each item will be a {@link RegResource}
     */
    public ListSelectionModel getResourceSelectionModel() {
        return regTable_.getSelectionModel();
    }

    /**
     * Returns the selection model used by the user to select capability items
     * from a completed query.
     *
     * @return   selection model
     *           (each item will be a {@link RegCapabilityInterface})
     */
    public ListSelectionModel getCapabilitySelectionModel() {
        return capTable_.getSelectionModel();
    }

    /**
     * Displays a user-directed message in the panel which contains the
     * results table.  This will be obliterated when a query starts or
     * completes; it is intended to contain advice for the user before
     * any query has been initiated.
     *
     * @param  lines   lines of message text (one element per screen line)
     */
    public void displayAdviceMessage( String[] lines ) {
        Box linesBox = Box.createVerticalBox();
        linesBox.add( Box.createVerticalGlue() );
        for ( int i = 0; i < lines.length; i++ ) {
            Box line = Box.createHorizontalBox();
            line.add( new JLabel( lines[ i ] ) );
            line.add( Box.createHorizontalGlue() );
            linesBox.add( line );
        }
        linesBox.add( Box.createVerticalGlue() );
        JComponent adviceBox = Box.createHorizontalBox();
        adviceBox.add( Box.createHorizontalGlue() );
        adviceBox.add( linesBox );
        adviceBox.add( Box.createHorizontalGlue() );
        resScroller_.setViewportView( adviceBox );
    }

    /**
     * Returns the action for submitting the query described by this
     * component's current state.
     *
     * @return  submit query action
     */
    public Action getSubmitQueryAction() {
        return submitQueryAction_;
    }

    /**
     * Constructs a menu which allows the user to select which attributes
     * of each displayed resource are visible.
     *
     * @param   name  menu name
     */
    public JMenu makeColumnVisibilityMenu( String name ) {
        return ((MetaColumnModel) regTable_.getColumnModel())
              .makeCheckBoxMenu( name );
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        for ( Iterator it = activeItems_.iterator(); it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof Action ) {
                ((Action) item).setEnabled( enabled );
            }
            else if ( item instanceof Component ) {
                ((Component) item).setEnabled( enabled );
            }
            else {
                assert false;
            }
        }
    }

    /**
     * Configures this component to be working on a query or not.
     * If <tt>message</tt> is non-null, it is displayed to the user,
     * and normal interaction is suspended.  Otherwise, normal interaction
     * is resumed.
     *
     * @param  message  user-visible text or null for ready status
     * @return  the progress bar which is used for the working display
     */
    private JProgressBar setWorking( String message ) {
        final JProgressBar progBar;
        boolean working = message != null;
        if ( ! working ) {
            resScroller_.setViewportView( dataPanel_ );
            progBar = null;
        }
        else {
            regTable_.setData( new RegResource[ 0 ] );
            if ( capTableModel_ != null ) {
                capTableModel_
                    .setCapabilities( new RegCapabilityInterface[ 0 ] );
            }
            JComponent msgLine = Box.createHorizontalBox();
            msgLine.add( Box.createHorizontalGlue() );
            msgLine.add( new JLabel( message ) );
            msgLine.add( Box.createHorizontalGlue() );

            JComponent progLine = Box.createHorizontalBox();
            progBar = new JProgressBar();
            progBar.setIndeterminate( true );
            progLine.add( Box.createHorizontalGlue() );
            progLine.add( progBar );
            progLine.add( Box.createHorizontalGlue() );

            JComponent workBox = Box.createVerticalBox();
            workBox.add( Box.createVerticalGlue() );
            workBox.add( msgLine );
            workBox.add( Box.createVerticalStrut( 5 ) );
            workBox.add( progLine );
            workBox.add( Box.createVerticalGlue() );

            workingPanel_.removeAll();
            workingPanel_.add( workBox );
            resScroller_.setViewportView( workingPanel_ );
        }
        setEnabled( ! working );
        cancelQueryAction_.setEnabled( working );
        return progBar;
    }
}
