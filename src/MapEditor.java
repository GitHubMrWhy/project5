import javax.swing.*; 
import javax.swing.filechooser.*; 
import java.awt.*; 
import java.awt.event.*; 
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.*; 
import java.io.*; 
import javax.swing.event.*; 
//import javax.swing.text.*; 
import javax.swing.border.*; 
import javax.swing.colorchooser.*; 
import javax.swing.filechooser.*; 
import javax.accessibility.*; 
import javax.imageio.*;
import java.awt.image.*; 
import java.beans.*; 
import java.applet.*; 
import java.net.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;


public class MapEditor extends JFrame implements ActionListener
{
  //Constants
  public static final int PREFERRED_WIDTH = 680;
  public static final int PREFERRED_HEIGHT = 600;
  public static final double MAX_ZOOM = 50.0;
  public static final double MIN_ZOOM = 11.0;
  public static final double ZOOM_INCREMENT = 3.0;
  public static final int TOLERANCE = 10;
  public static int mode=0;
  //GUI components
  private JFrame directionsFrame, locationFrame;
  private JScrollPane scrollPane;
  private ZoomPane zoomPane;
  private MapScene map;
  private JPopupMenu popup;
  
  //Menu items for file menu:
  private JMenuItem exitAction;
  private JMenuItem openAction;
  private JMenuItem saveAction;
  private JMenuItem saveAsAction;
  private JMenuItem newAction;
  
  //Menu items for map menu:
  private JMenu mapMenu;
  private JMenuItem zoomInAction;
  private JMenuItem zoomOutAction;
  
  
  //Menu items for directions menu:
  private JMenu directionsMenu;
  private JMenuItem directionsAction;
  private JMenuItem mstAction;
  
  //Menu items for property  menu:
   private JMenu property ;
  
  //Menu items for help menu:
  private JMenuItem aboutAction;
  private JMenuItem helpAction;
  
  //Menu items for the right-click menu:
  private JMenuItem edit_rightClick;
  private JMenuItem delete_rightClick;
  
  //Items for directions frame
  private JComboBox fromMenu, toMenu;
  private JLabel fromLabel, toLabel;
  private JButton getDirections, directionsCancel;
  
  //Items for edit location frame
  private JLabel nameLabel, IDLabel, pointLabel;
  private JTextField nameField;
  private JButton saveLocation;
  
  
  public static JCheckBox printNames;
  
  //Session variables
  // public static ArrayList<Vertex> points = new ArrayList<Vertex>();
  // public static ArrayList<Path> paths = new ArrayList<Path>();
  //Map location
  public static String dir = "Resources/"; 
  public static String imagePath = "purdue-map.jpg"; //Default map image location
  public static String filePath = ""; //Default xml location
  XML mapXML = new XML();
  int vertex_id = 0;
  double zoomValue = 20.00;
  public static double scale_feet_per_pixel = 1.0;
  
  //Temporary variables
  Point p;
  //Vertex rightClicked = null;
  
  public static void main(String[] args) 
  { 
    MapEditor mapEditor = new MapEditor(); 
    mapEditor.setVisible(true);
  } 
  
  //Handle events for menu objects
  public void actionPerformed(ActionEvent evt)
  {
    //Actions for file menu:
    if(evt.getSource().equals(exitAction)) //Exit program
    {
      System.exit(0);
    }
    else if(evt.getSource().equals(newAction)) //Create new map
    {
      String response = null;
      String tmpPath = imagePath;
      String tmpDir = dir;
      imagePath = null;
      double tmp_scale_feet_per_pixel = scale_feet_per_pixel;
      scale_feet_per_pixel = 0.0;
      boolean done = false;
      
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileFilter(new FileNameExtensionFilter("Valid Map Files", "jpg", "gif"));
      int result = fileChooser.showOpenDialog(this);
      
      if(result == JFileChooser.APPROVE_OPTION)
      {
        imagePath = fileChooser.getSelectedFile().getName();
        dir = fileChooser.getCurrentDirectory().getAbsolutePath();
        
        done = false;
        if(imagePath != null)
        {
          while(!done)
          {
            response = JOptionPane.showInputDialog(null, "Enter the feet-per-pixel constant: ", "New Map", JOptionPane.OK_CANCEL_OPTION);
            
            if(response == null)
            {
              break;
            }
            
            try
            {
              scale_feet_per_pixel = Double.parseDouble(response);
              done = true;
            }
            catch(Exception e)
            {
              JOptionPane.showMessageDialog(null, "Invalid feet-per-pixel constant.", "Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        }
      }
      
      
      if(response != null && imagePath != null)
      {
        clearMap();
        loadImage();
        //JOptionPane.showMessageDialog(null, "New map successfully created!", "New Map", JOptionPane.PLAIN_MESSAGE);
      }
      else
      {
        imagePath = tmpPath;
        scale_feet_per_pixel = tmp_scale_feet_per_pixel;
        dir = tmpDir;
      }
    }
    else if(evt.getSource().equals(openAction)) //Open existing XML 
    {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileFilter(new FileNameExtensionFilter("Valid Map Files", "xml"));
      int result = fileChooser.showOpenDialog(this);
      
      if(result == JFileChooser.APPROVE_OPTION)
      {
        clearMap();
        dir = fileChooser.getCurrentDirectory().getAbsolutePath();
        filePath = fileChooser.getSelectedFile().getAbsolutePath();
        mapXML.openMap(filePath);
        // mapXML.setBitmap(filePath);
        loadImage();
        
        saveAction.setEnabled(true);
      }
      
    }
    else if(evt.getSource().equals(saveAction)) //Save current XML
    {
      if(filePath != null && imagePath != null)
      {
        mapXML.saveMap(filePath, imagePath, scale_feet_per_pixel);
      }
    }
    else if(evt.getSource().equals(saveAsAction)) //Save current XML with different name
    {
      JFileChooser fileChooser = new JFileChooser();
      int result = fileChooser.showSaveDialog(this);
      
      if(result == JFileChooser.APPROVE_OPTION)
      {
        filePath = fileChooser.getSelectedFile().getAbsolutePath();
        if(!filePath.contains(".xml"))
        {
          filePath += ".xml";
        }
        mapXML.saveMap(filePath, imagePath, scale_feet_per_pixel);
        saveAction.setEnabled(true);
      }
      
    }
    
    //Actions for map menu:
    else if(evt.getSource().equals(zoomInAction)) //Zoom in
    {
      if(zoomValue < MAX_ZOOM)
      {
        zoomOutAction.setEnabled(true);
        double scale = (zoomValue + ZOOM_INCREMENT) / 20.0;
        zoomValue+=ZOOM_INCREMENT;
        zoomPane.zoom(scale);
        zoomPane.repaint();
      }
      else
      {
        zoomInAction.setEnabled(false);
      }
    }
    else if(evt.getSource().equals(zoomOutAction)) //Zoom out
    {
      if(zoomValue > MIN_ZOOM)
      {
        zoomInAction.setEnabled(true);
        double scale = (zoomValue - ZOOM_INCREMENT) / 20.0;
        zoomValue-=ZOOM_INCREMENT;
        zoomPane.zoom(scale);
        zoomPane.repaint();
      }
      else
      {
        zoomOutAction.setEnabled(false);
      }
    }
    
    //Actions for directions menu
    else if(evt.getSource().equals(directionsAction))
    {
      resetPaths();
      fromMenu.removeAllItems();
      toMenu.removeAllItems();
      fromMenu.addItem("-----");
      toMenu.addItem("-----");
      int max_size = 5;
      
   
     for (int i= 0 ; i<MapScene.count;i++){
       if(MapScene.loc[i].pt!=null){
       
       fromMenu.addItem(Integer.toString(MapScene.loc[i].id)); 
       }
     }
      for (int i= 0 ; i<MapScene.count;i++){
       if(MapScene.loc[i].pt!=null){
       toMenu.addItem(Integer.toString(MapScene.loc[i].id));
       }
      }
      
      directionsFrame.setSize((295 + max_size),195);
      directionsFrame.setVisible(true);
     this.setEnabled(false);
     directionsFrame.toFront();
    }
    else if(evt.getSource().equals(mstAction))
    {
      MapViewer dj_quest = new MapViewer();
      // TreeSet<Path> stree = dj_quest.MST();
      // for(Path p : stree)
      // {
      //   p.isMSTEnabled = true;
      // }
      map.upperLeftScroll = zoomPane.getUpperLeft();
      map.mstCalculated(dj_quest.getMSTLength());
      map.mouseMoved();
    }
    else if(evt.getSource().equals(directionsCancel))
    {
      handleClose();
    }
  }
  
  public boolean verifyFile(String fp)
  {
    try
    {
      File tmp = new File(fp);
    }
    catch(Exception e)
    {
      return false;
    }
    return true;
  }
  
  public void clearMap()
  {
    for (int i= 0 ; i<MapScene.count;i++){
      
      MapScene.loc[i].pt=null;
      // System.out.println(MapScene.loc[i].pt);
      
    }
    for (int i=0;i<MapScene.pNum;i++){
      MapScene.line[i]._statPoint=null;
      MapScene.line[i]._endPoint=null;
      
      
    }
    
  }
  
  public void handleClose()
  {
   /* if(locationFrame.isVisible())
    {
      /* for(Vertex v : points)
       {
       v.isSelected = false;
       }
    }
    directionsFrame.setVisible(false);
    locationFrame.setVisible(false);
    this.setEnabled(true);
    this.toFront();
    */
  }
  
  public void resetPaths()
  {
    /* for(Path p : paths)
     {
     p.isDirectionEnabled = false;
     p.isMSTEnabled = false;
     p.isSelected = false;
     }*/
    map.directions = false;
    map.mst = false;
    map.mouseMoved();
  }
  
  public void loadImage()
  {
    Image image = null;
    if(dir.contains("/"))
    {
      image = new ImageIcon(dir + "/" + imagePath).getImage();
    }
    else
    {
      image = new ImageIcon(dir + "\\" + imagePath).getImage();
    }
    getContentPane().remove(zoomPane);
    map.setImage(image);
    zoomPane.setScene(map);
    getContentPane().add(zoomPane);
    zoomPane.repaint();
  }
  
  
  
  
  public MapEditor() 
  {
    setTitle("MapEditor");
    setSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
    setBackground(Color.gray);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    JPanel panel = new JPanel();
    panel.setLayout( new BorderLayout()); 
    getContentPane().add(panel);
    //combo box
    DefaultComboBoxModel model = new DefaultComboBoxModel();
    model.addElement("Please select a mode");
    model.addElement("Insert Location Mode");
    model.addElement("Delete Location Mode");
    model.addElement("Insert Path Mode");
    model.addElement("Delete Path Mode");
    
    JComboBox comboBox = new JComboBox(model);
    comboBox.addActionListener(new ActionListener() {
      
      public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox)e.getSource();
        String modeName = (String)cb.getSelectedItem();
        
        if (  modeName.equals("Insert Location Mode"))
          mode=1;
        
        else if  (modeName.equals("Delete Location Mode"))
          mode=2;
        
        else if  (modeName.equals( "Insert Path Mode"))
          mode=3;
        
        else if  (modeName.equals( "Delete Path Mode"))
          mode=4;
        else mode=0;       
        
        
        
        System.out.println("mode: "+mode);
      }
    });
    
    
    //Create and set up menu bars:
    JMenuBar menubar = new JMenuBar();
    JMenu fileMenu = new JMenu("File");
    property = new JMenu("Location Properties");
     property.addMenuListener(new MenuListener() {

    public void menuSelected(MenuEvent e) {
       System.out.println("menuSelected");
    int n=0;
    for ( int i=0;i<MapScene.count;i++){

    if (MapScene.loc[i].pt!=null)
    n++;

    }
    if (n!=0){
    String[] id = new String [n];
    n=0;
    for ( int i=0;i<MapScene.count;i++){
    if (MapScene.loc[i].pt!=null){
    id[n]=Integer.toString(i);
    n++;
    }

    }

    String ask = (String) JOptionPane.showInputDialog(null, 
      "Please select location id to show the properties",
      "Location Properties",
      JOptionPane.QUESTION_MESSAGE, 
      null, 
      id, 
      id[0]);
    try{
    int askid=Integer.parseInt(ask);
    
    JOptionPane.showInputDialog(null, "Name: "+MapScene.loc[askid].name+"\nx: "+MapScene.loc[askid].pt.x+"\ny: "+MapScene.loc[askid].pt.y+"\nid: "+MapScene.loc[askid].id+"\nYou can chaneg name of the location below", 
      "properties of the location", 1);
    } catch (NumberFormatException nfe) {
               JOptionPane.showMessageDialog(null,"Input must be a number.");
            }
    }else{
      JOptionPane.showMessageDialog(null, "Please insert locaton first");
    }


    }

    public void menuDeselected(MenuEvent e) {
     System.out.println("menuDeselected");

    }

    public void menuCanceled(MenuEvent e) {
     System.out.println("menuCanceled");

    }
  });
    mapMenu = new JMenu("Map");
    //JMenu helpMenu = new JMenu("Help");
    popup = new JPopupMenu();
    //property Menu setup
   // property.addActionListener(this); 
    
    
    
    
    //Directions Frame Setup
    directionsFrame = new JFrame("Directions");
    GridLayout frameLayout = new GridLayout(3,2);
    JPanel toPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JPanel fromPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    directionsFrame.setLayout(frameLayout);
    
    fromMenu = new JComboBox();
    toMenu = new JComboBox();
    fromLabel = new JLabel("From: ");
    toLabel = new JLabel  ("To:     ");
    getDirections = new JButton("Get Directions");
    directionsCancel = new JButton("Cancel");
    directionsCancel.addActionListener(this);
    getDirections.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent evt)
      {
        if(fromMenu.getSelectedIndex() != 0 && toMenu.getSelectedIndex() != 0)
        {
          //Vertex from = null;
          // Vertex to = null;
          try
          {
            //from = points.get(fromMenu.getSelectedIndex() - 1);
            //to = points.get(toMenu.getSelectedIndex() - 1);
            MapViewer dijkstra = new MapViewer();
            //dijkstra.initiateDirections(from);
            // LinkedList<Vertex> vertices = dijkstra.getDirections(to);
            double totalDistance = 0.0;
            
            //Vertex prev = null;
            
            /* for(Vertex v : vertices)
             {
             
             if(prev == null)
             {
             prev = v;
             continue;
             }
             
             //Make path between v and prev green and also make v green
             for(Path p : paths)
             {
             if((p.getStart().equals(prev) && p.getEnd().equals(v)) || (p.getEnd().equals(prev) && p.getStart().equals(v)))
             {
             p.isDirectionEnabled = true;
             totalDistance+=p.getWeight();
             }
             
             }
             
             prev = v;
             }
             */
            
            handleClose();
            map.upperLeftScroll = zoomPane.getUpperLeft();
            map.directionsCalculated(totalDistance);
          }
          catch(Exception e)
          {
            //  JOptionPane.showMessageDialog(null, "A path from \"" + from.getName() + "\" to \"" + to.getName() + "\" does not exist.", "Directions", JOptionPane.PLAIN_MESSAGE);
          }
        }
      }
    });
    //getDirections.setIcon(new ImageIcon("Resources/directions.gif"));
    //directionsCancel.setIcon(new ImageIcon("Resources/cancel.gif"));
    fromPanel.add(fromLabel);
    fromPanel.add(fromMenu);
    toPanel.add(toLabel);
    toPanel.add(toMenu);
    buttonPanel.add(getDirections);
    buttonPanel.add(directionsCancel);
    
    directionsFrame.setSize(295,180);
    directionsFrame.setResizable(false);
    directionsFrame.add(fromPanel);
    directionsFrame.add(toPanel);
    directionsFrame.add(buttonPanel);
    directionsFrame.setLocationRelativeTo(null); 
    directionsFrame.addWindowListener(new WindowAdapter() {
      
      public void windowClosing(WindowEvent e) {
        handleClose();
      }
    });
    
    //Menu items for file menu:
    exitAction = new JMenuItem("Exit");
    exitAction.addActionListener(this);
    
    openAction = new JMenuItem("Open");
    openAction.addActionListener(this);
    
    saveAction = new JMenuItem("Save");
    saveAction.addActionListener(this);
    
    saveAction.setEnabled(false);
    newAction = new JMenuItem("New");
    newAction.addActionListener(this);
    
    saveAsAction = new JMenuItem("Save As...");
    saveAsAction.addActionListener(this);
    
    fileMenu.add(newAction);
    fileMenu.add(openAction);
    fileMenu.add(saveAction);
    fileMenu.add(saveAsAction);
    fileMenu.add(exitAction);
    
    //Menu items for map menu:
    zoomInAction = new JMenuItem("Zoom In");
    zoomInAction.addActionListener(this);
    
    zoomOutAction = new JMenuItem("Zoom Out");
    zoomOutAction.addActionListener(this);
    
    ButtonGroup modeOptions = new ButtonGroup();
    
    //directionsMenu = new JMenu("Directions");
    directionsAction = new JMenuItem("Get Directions");
    directionsAction.addActionListener(this);
    
    mstAction = new JMenuItem("Calculate MST");
    mstAction.addActionListener(this);
    
    mapMenu.add(zoomInAction);
    mapMenu.add(zoomOutAction);
    
    mapMenu.addSeparator();
    mapMenu.add(directionsAction);
    mapMenu.add(mstAction);
    
    menubar.add(fileMenu);
    menubar.add(mapMenu);
    menubar.add(property);
    menubar.add(comboBox);
    
    printNames = new JCheckBox("Display Location Names");
    printNames.setSelected(false);
    printNames.addActionListener(this);
    
    setJMenuBar(menubar);
    setLocationRelativeTo(null); 
    
    Image image = null;
    if(dir.contains("/"))
    {
      image = new ImageIcon(dir + "/" + imagePath).getImage();
    }
    else
    {
      image = new ImageIcon(dir + "\\" + imagePath).getImage();
    }
    map = new MapScene(image);
    zoomPane = new ZoomPane(map);
    zoomPane.setMap(map);
    
    
    MouseAdapter listener = new MouseAdapter() {
      
      public void mouseClicked(MouseEvent e)
      {
        Point point = zoomPane.toViewCoordinates(e.getPoint());
        map.mouseClicked(point,e);
        
      }
      public void mousePressed(MouseEvent e) 
      {
        Point point = zoomPane.toViewCoordinates(e.getPoint());
        map.mousePressed(point);
        
      }    
      public void mouseReleased(MouseEvent e)
      {
        Point point = zoomPane.toViewCoordinates(e.getPoint());
        map.mouseReleased(point);
      }
      
    };
    
    MouseMotionAdapter motionListener = new MouseMotionAdapter() {
      public void mouseDragged(MouseEvent e) 
      {
        Point point = zoomPane.toViewCoordinates(e.getPoint());
        map.mouseDragged(point);
      }
      
    };
    
    zoomPane.getZoomPanel().addMouseListener(listener);
    zoomPane.getZoomPanel().addMouseMotionListener(motionListener);
    
    getContentPane().add(zoomPane);
    
  }
  
  
};
