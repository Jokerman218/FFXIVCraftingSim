package application;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import application.components.ConfigManager;
import application.components.LogManager;
import application.components.ReplayManager;
import application.components.SkillIcon;
import application.components.Timer;
import application.subPane.AdvancedSettingsPane;
import application.subPane.CraftingHistoryPane;
import application.subPane.EditModePane;
import engine.CraftingStatus;
import engine.Engine;
import engine.EngineStatus;
import exceptions.CraftingException;
import exceptions.ExceptionStatus;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import skills.ActiveBuff;
import skills.Buff;
import skills.BuffSkill;
import skills.PQSkill;
import skills.Skill;
import skills.SpecialSkills;

/**
 * The main scene of the program
 * @author keithMaxwell ����-���������
 *
 */
public class ViewManagerPC extends ViewManager
{
	private static final double WIDTH = 850; 			// The width of the scene 
	private static final double HEIGHT = 710;			// The height of the scene 
	private static final double REC_WIDTH = 800;		// Width of the panes
	private static final double EDGE_GENERAL = 4.0;		// The general edge width of panes
	private static final double SKILL_HEIGHT = 270;		// The height of skill pane
	private static final double BAR_EDGE = 1.5;			// The width of progress and quality bars
	private static final double BAR_WIDTH = 400.0;		// The width of the bars
	private static final double BAR_HEIGHT = 30.0;		// The height of the bars
	private static final double CP_EDGE = 1.5;			// CP bar edge width
	private static final double CP_WIDTH = 150.0;		// CP bar width
	private static final double CP_HEIGHT = 15.0;		// CP bar height
		
//	private static final Color TEXT_COLOR = Color.BLACK; // The general color of the text
	
	private Stage stage;						// Main stage
	private Scene mainScene;					// Main scene of the stage
	private AnchorPane mainPane;				// Main pane of the scene, covers everything
	private AnchorPane lastSkillAp;				// The anchor pane that displays the last skill used
	private AnchorPane recSkillAp;				// The anchor pane that recommend the next skill to use
	private GridPane iconContainer;				// The pane that stores all the skill icons/buttons
	private VBox mainContainer;					// The container that stores other panes
	private HBox buffContainer;					// The container that display buffs
	private Circle statusDisp;					// The circle that displays the crafting status
	private GridPane efficiencyDisp;				// The text that displays current efficiency
	private Text durabilityText;				// The text that displays current durability
	private Text round;							// The text that displays current round
	private Text skillDescription;				// The text that displays the skill(where the cursor points) description
	private Text finalizeText;					// The text that notifies the user that present recommend skills are in finalize sequence
	private Timeline tml = new Timeline();		// The timeline that stores GCD animation
	private Button confirm;
	private ArrayList<Text> progText;		// 0=>Progress 1=>Quality 2=>CP 3=>Status 4=>Success
	private ArrayList<Rectangle> bars; 		// 0=>Progress 1=>Quality 2=>CP
	private ArrayList<TextField> inputTf; 	// The ArrayList that stores the TextFields for input
	private ArrayList<SkillIcon> skillIcons;	// ArrayList that stores all skillIcon objects
												// Makes it easier to operate
	private ConfigManager cm; 					// The config manager that loads/saves the config
	private LogManager lm;
	private LogManager.Node node;
	
	private CraftingHistoryPane ch;				
	private AdvancedSettingsPane asp;			
	private EditModePane emp;
	
	private String version;
	private boolean finish;
	private boolean hasUpdate;
		
	public ViewManagerPC() {
		engine = new Engine(craftsmanship, control, cp, totalDurability, totalProgress, totalQuality, 
				rCraftsmanship, rControl, progressDifference, 
				qualityDifference, seed, CraftingStatus.Mode.Expert);
		lm = engine.getLogManager();
		lm.setViewManager(this);
		progText = new ArrayList<>();
		bars = new ArrayList<>();
		tm = new Timer();
		skillIcons = new ArrayList<>();
		inputTf = new ArrayList<>();
		
		cm = new ConfigManager(this, engine);
		mainPane = new AnchorPane();
		
		version = "";
		finish = false;
		hasUpdate = false;

		tm.startTimer();		
		
		initSkillsList();
		initMainDisplay();
		initStage();
		
		confirm.requestFocus();
//		cm.importConfig(false);
		
		ch = new CraftingHistoryPane(this);  // the CraftingHistoryPane need the size of the 
											 // main stage so it's initialized at last
				
		tml.setOnFinished(e -> {
			updateAll();
			ch.addToQueue(node.getSkill(), node.getCraftingStatus(), node.isSkillSuccess());
		});
	}
	
	protected void showAbout() {
		checkUpdate();
		
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("");
		alert.setHeaderText("����");
		
		VBox content = new VBox();
		
//		Hyperlink hl = new Hyperlink("  ff.web.sdo.com/talos");
//		hl.setOnMouseClicked(e -> {
//			try
//			{
//				Desktop.getDesktop().browse(new URI("https://ff.web.sdo.com/talos"));
//			} catch (IOException e1)
//			{
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			} catch (URISyntaxException e1)
//			{
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		});
		
		
		TextField l0 = new TextField("���°汾�ˣ��������������ǰ��nga���ظ���");
		Hyperlink h0 = new Hyperlink("  NGA������");
		h0.setOnMouseClicked(e -> {
			try
			{
				Desktop.getDesktop().browse(new URI("https://bbs.nga.cn/read.php?tid=21082240"));
			} catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (URISyntaxException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		
		TextField l1 = new TextField("������ʱ�䣺2020-12-30");
		TextField l2 = new TextField("�Ƽ����ܹ��ܽ����ο�������Ĭ�ϼ������֣�������Ҫ�����Լ���ϰ�����ַ����ƽ�");
		TextField l3 = new TextField("��������⻶ӭ�ڷ����������Ի�˽���ң��Ҷ��ῴ��");
//		TextField l4 = new TextField("�������ģ�����а���������֧���ҵĻ����͵��������������ҵ�ħ�󳵰ɣ�лл��");
//		TextField l5 = new TextField("���ƺ�: mkc14360610");
//
//		if(finish) {
//			l4.setText("�ĸ���λ���ˣ�лл��λ��");
//		}
		
		l1.setEditable(false);
		l2.setEditable(false);
		l3.setEditable(false);
//		l4.setEditable(false);
//		l5.setEditable(false);
		
		l0.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		l1.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		l2.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		l3.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
//		l4.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
//		l5.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		
		l0.getStyleClass().add("copyablelabel");
		l1.getStyleClass().add("copyablelabel");
		l2.getStyleClass().add("copyablelabel");
		l3.getStyleClass().add("copyablelabel");
//		l4.getStyleClass().add("copyablelabel");
//		l5.getStyleClass().add("copyablelabel");
		
		int i = 0;
		
		if(hasUpdate) {
			content.getChildren().add(i++, l0);
			content.getChildren().add(i++, h0);
		}
		content.getChildren().add(i++, l1);
		content.getChildren().add(i++, l2);
		content.getChildren().add(i++, l3);
//		content.getChildren().add(i++, l4);
//		if(!finish) {
//			content.getChildren().add(i++, l5);
//			content.getChildren().add(i++, hl);
//		}

		content.setMinWidth(500);
		
		alert.getDialogPane().setExpandableContent(content);
		
		alert.getDialogPane().setMinWidth(700);
		alert.setWidth(700);
		
		alert.getDialogPane().setExpanded(true);
		alert.setResizable(false);
		alert.showAndWait();
	}
	
	private void checkUpdate() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse("info.xml");
            NodeList n = document.getElementsByTagName("Node");
            
            NodeList childNodes = n.item(0).getChildNodes();
                        
            for (int k = 0; k < childNodes.getLength(); k++) {
                if (childNodes.item(k).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    if(childNodes.item(k).getNodeName().equals("version")) {
                    	version = childNodes.item(k).getFirstChild().getNodeValue();
                    }
                    if(childNodes.item(k).getNodeName().equals("finish")) {
                    	Object o = childNodes.item(k).getFirstChild().getNodeValue();
                    	finish = (o.equals("false") ? false : true);
                    }
                }
            }
		}
		catch (ParserConfigurationException e) {
            e.printStackTrace();
        } 
		catch (IOException e) {
            e.printStackTrace();
        } 
		catch (SAXException e)
		{
			e.printStackTrace();
		}
		finally {
			
		}
		
		FileOutputStream fileOut = null;
		HttpURLConnection connection = null;
		InputStream inputStream = null;
		
		try {
			 URL httpUrl=new URL("http://ffxiv.cf/crafter/update/info.xml");
			 connection=(HttpURLConnection) httpUrl.openConnection();
			 connection.setRequestMethod("GET");
		     connection.setDoInput(true);  
		     connection.setDoOutput(true);
		     connection.setUseCaches(false);
		     connection.connect();
		     inputStream=connection.getInputStream();
		     BufferedInputStream bis = new BufferedInputStream(inputStream);
		     String filePath = "./";
	         fileOut = new FileOutputStream(filePath+"cache");
	         BufferedOutputStream bos = new BufferedOutputStream(fileOut);
	         
	         byte[] buf = new byte[4096];
	         int length = bis.read(buf);
	         while(length != -1)
	         {
	        	 bos.write(buf, 0, length);
	        	 length = bis.read(buf);
	         }
	         bos.close();
	         bis.close();
	         connection.disconnect();
	         
	         try {
	 			DocumentBuilder db = dbf.newDocumentBuilder();
	             Document document = db.parse("cache");
	             NodeList n = document.getElementsByTagName("Node");
	             
	             NodeList childNodes = n.item(0).getChildNodes();
	                         
	             for (int k = 0; k < childNodes.getLength(); k++) {
	                 if (childNodes.item(k).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
	                     if(childNodes.item(k).getNodeName().equals("version")) {
	                     	String tVersion = childNodes.item(k).getFirstChild().getNodeValue();
	                     	if(!tVersion.equals(version)) {
	                     		hasUpdate = true;
	                     	}
	                     }
	                     if(childNodes.item(k).getNodeName().equals("finish")) {
	                     	Object o = childNodes.item(k).getFirstChild().getNodeValue();
	                     	finish = (o.equals("false") ? false : true);
	                     }
	                 }
	             }
	 		}
	 		catch (ParserConfigurationException e) {
	             e.printStackTrace();
	         } 
	 		catch (IOException e) {
	             e.printStackTrace();
	         } 
	 		catch (SAXException e)
	 		{
	 			// TODO Auto-generated catch block
	 			e.printStackTrace();
	 		}
	 		finally {
	 			
	 		}
	         
		} 
		catch (Exception e)
		{
			 e.printStackTrace();
		}

		System.out.println(hasUpdate);
		
		return;
	}
	
	/**
	 * manually define the category of each skill......
	 */
	private void initSkillsList() {
		progressSkills = new ArrayList<>();
		qualitySkills = new ArrayList<>();
		buffSkills = new ArrayList<>();
		recoverySkills = new ArrayList<>();
		otherSkills = new ArrayList<>();
		
		progressSkills.add(PQSkill.Basic_Synthesis);
		progressSkills.add(PQSkill.Careful_Synthesis);
		progressSkills.add(PQSkill.Rapid_Synthesis);
		progressSkills.add(PQSkill.Groundwork);
		progressSkills.add(PQSkill.Focused_Synthesis);
		progressSkills.add(PQSkill.Brand_of_the_Elements);
		progressSkills.add(PQSkill.Intensive_Synthesis);
		progressSkills.add(PQSkill.Delicate_Synthesis);
		
		qualitySkills.add(PQSkill.Basic_Touch);
		qualitySkills.add(PQSkill.Standard_Touch);
		qualitySkills.add(PQSkill.Hasty_Touch);
		qualitySkills.add(PQSkill.Precise_Touch);
		qualitySkills.add(PQSkill.Focused_Touch);
		qualitySkills.add(PQSkill.Patient_Touch);
		qualitySkills.add(PQSkill.Prudent_Touch);
		qualitySkills.add(PQSkill.Preparatory_Touch);
		qualitySkills.add(SpecialSkills.Byregots_Blessing);
		
		buffSkills.add(BuffSkill.Muscle_Memory);
		buffSkills.add(BuffSkill.Reflect);
		buffSkills.add(BuffSkill.Inner_Quiet);
		buffSkills.add(BuffSkill.Waste_Not);
		buffSkills.add(BuffSkill.Waste_Not_II);
		buffSkills.add(BuffSkill.Great_Strides);
		buffSkills.add(BuffSkill.Innovation);
		buffSkills.add(BuffSkill.Veneration);
		buffSkills.add(BuffSkill.Name_of_the_Elements);
		buffSkills.add(BuffSkill.Final_Appraisal);
		
		recoverySkills.add(SpecialSkills.Masters_Mend);
		recoverySkills.add(BuffSkill.Manipulation);
		
		otherSkills.add(SpecialSkills.Observe);
		otherSkills.add(SpecialSkills.Tricks_of_the_Trade);
		otherSkills.add(SpecialSkills.Careful_Observation);
	}
	
	/**
	 * initiate the main stage
	 */
	private void initStage() {
		stage = new Stage();

		mainScene = new Scene(mainPane, WIDTH, HEIGHT);
		
		mainPane.setBackground(new Background(
				new BackgroundFill(Color.LIGHTGRAY, null, null)));

		stage.setTitle("FFXIV Crafting Simulator " + version);
		stage.setScene(mainScene);
		stage.setResizable(false);
		stage.setOnCloseRequest(e -> {			// Close other related windows
			closeSubPanes(true);
		});

		mainScene.setOnKeyPressed(e -> {
			System.out.println(e.getCode());
			for(SkillIcon si: skillIcons) {
				String temp = "";
				if(e.isShiftDown()) {
					temp = "s";
				} else if(e.isAltDown()) {
					temp = "a";
				} else if(e.isControlDown()) {
					temp = "c";
				} else {
					temp = "n";
				}
				if(e.getCode().toString().length() > 1) {
					temp += e.getCode().toString().substring(5);
				} else {
					temp += e.getCode().toString();
				}
				if(si.getKeyCodeCombination() != null) {
					if(si.match(temp)) {
						si.fireButton();
					}
				}
			}
		});
	}
	
	/**
	 * initiate the main display contents
	 */
	private void initMainDisplay() {
		mainContainer = new VBox(20);
		
		mainContainer.setSpacing(10);
		mainContainer.setFillWidth(false);
		mainContainer.setAlignment(Pos.CENTER_LEFT);
		
		mainPane.getChildren().add(mainContainer);

		mainContainer.getChildren().add(initInput());
		mainContainer.getChildren().add(initInfoDisplay());
//		mainContainer.getChildren().add(initCPDisplay());
//		mainContainer.getChildren().add(initEffAndBuffDisplay());
//		mainContainer.getChildren().add(initBuffDisp());
		mainContainer.getChildren().add(initSkills());

		VBox.setMargin(efficiencyDisp, new Insets(0, 0, 0 , 140.0));
		
		AnchorPane.setTopAnchor(mainContainer, 10.0);
		AnchorPane.setLeftAnchor(mainContainer, 30.0);
		
		engine.setEngineStatus(EngineStatus.Pending);
	}
	
	/**
	 * initiate the input section
	 * @return the pane of input section
	 */
	private Node initInput() {		
		double tfWidth = 70.0;
		GridPane gp = new GridPane();
		GridPane border = new GridPane();
		GridPane back = new GridPane();
//		Button confirm = new Button("ȷ��");
		confirm = new Button("ȷ��");
		Button logs = new Button("��־"); 
		Button advanced = new Button("�߼�����");
		Button finish = new Button("��������");
		Button iconRearr = new Button("�༭ͼ��");
		Button loadConfig = new Button("��������");
		Button saveConfig = new Button("��������");
		Button saveReplay = new Button("����¼��");
		Button loadReplat = new Button("��ȡ¼��");
		
		ArrayList<Text> t = new ArrayList<Text>();
		
		Text craftT = new Text("��������");
		Text controlT = new Text("�ӹ�����");
		Text CPT = new Text("CP");
		Text totalProgT = new Text("�ܽ���");
		Text totalQltyT = new Text("��Ʒ��");
		Text totalDuraT = new Text("���;�");
		
		TextField craftTf = new TextField(Integer.toString(craftsmanship));
		TextField controlTf = new TextField(Integer.toString(control));
		TextField CPTf = new TextField(Integer.toString(cp));
		TextField totalProgTf = new TextField(Integer.toString(totalProgress));
		TextField totalQltyTf = new TextField(Integer.toString(totalQuality));
		TextField totalDuraTf = new TextField(Integer.toString(totalDurability));

		CheckBox GCDCb = new CheckBox("GCD");
		
		ChoiceBox<String> cb = new ChoiceBox<String>();
		
		cb.getItems().add("����");
		cb.getItems().add("��ͨ");
		cb.getItems().add("��");
		
		inputTf.add(craftTf);
		inputTf.add(controlTf);
		inputTf.add(CPTf);
		inputTf.add(totalProgTf);
		inputTf.add(totalQltyTf);
		inputTf.add(totalDuraTf);
		
		gp.setHgap(5);
		gp.setVgap(3);
		
		gp.setPrefWidth(REC_WIDTH);
		
		GCDCb.setIndeterminate(false);
		GCDCb.setSelected(true);
		GCDCb.setTextFill(Color.WHITE);
		
		craftTf.setPrefWidth(tfWidth);
		controlTf.setPrefWidth(tfWidth);
		CPTf.setPrefWidth(tfWidth);
		totalProgTf.setPrefWidth(tfWidth);
		totalQltyTf.setPrefWidth(tfWidth);
		totalDuraTf.setPrefWidth(tfWidth);

		cb.setValue("����");
		
		// Define the action when confirm button is clicked
		confirm.setOnMouseClicked(e -> {
			CraftingStatus.Mode m = null;
			
			usedDebug = false;
			
			if(emp != null) {
				emp.close();
				if(emp.getHotkeyBindingPane() != null) {
					emp.getHotkeyBindingPane().close();
				}
				emp = new EditModePane(this, engine);
			}
			ch.destory();
			setLastSkill(null);
			
			craftsmanship = Integer.parseInt(craftTf.getText()); 
			control = Integer.parseInt(controlTf.getText()); 
			cp = Integer.parseInt(CPTf.getText());
			totalDurability = Integer.parseInt(totalDuraTf.getText());
			totalProgress = Integer.parseInt(totalProgTf.getText()); 
			totalQuality = Integer.parseInt(totalQltyTf.getText());
			ch = new CraftingHistoryPane(this);
			hasGCD = GCDCb.isSelected();
			
			if(cb.getValue().equals(cb.getItems().get(0))) {
				m = CraftingStatus.Mode.Expert;
			} else if(cb.getValue().equals(cb.getItems().get(1))) {
				m = CraftingStatus.Mode.Normal;
			} else {
				m = CraftingStatus.Mode.Testing;
			}
			
			engine = new Engine(craftsmanship, control, cp, totalDurability, totalProgress, totalQuality, 
					rCraftsmanship, rControl, progressDifference, 
					qualityDifference, seed, m);
			// Creates a new engine to restart everything
			lm = engine.getLogManager();
			lm.setViewManager(this);
			
			SkillIcon.setVm(engine, tml, this);
			updateAll();
			ch.display();
			
			closeSubPanes(false);
		});
		
		// Define the action when advanced settings button is clicked
		advanced.setOnMouseClicked(e -> {
			if(asp != null) {
				asp.close();
			}
			asp = new AdvancedSettingsPane(t, this);
			asp.display();
		});
		// Define the action when export logs button is clicked
		logs.setOnMouseClicked(e -> {
			if(ch != null) {
				ch.display();
			}
		});
		

		// Define the action when finish button is clicked
		finish.setOnMouseClicked(e -> {		
			if(engine.getEngineStatus() == EngineStatus.Crafting) {
				postFinishMessage(ExceptionStatus.Craft_Failed);
			}
		});
		
		// Define the action when rearrange icon mapping button is clicked
		iconRearr.setOnMouseClicked(e -> { 
			if(emp == null) {
				emp = new EditModePane(this, engine);
			} 
			emp.setEngine(engine);
			emp.display();
		});
		
		// Define the action when save / load config button is clicked
		saveConfig.setOnMouseClicked(e -> {
			cm.exportConfig();
		});
		
		loadConfig.setOnMouseClicked(e -> {
			cm.importConfig(true);
		});
		
		saveReplay.setOnMouseClicked(e -> {
			lm.saveReplay();
		});
		
		loadReplat.setOnMouseClicked(e -> {
			ReplayManager rm = new ReplayManager(this);
			try {
				rm.load();
			
			
				CraftingStatus.Mode m = CraftingStatus.Mode.Expert;
				
				usedDebug = false;
				
				if(emp != null) {
					emp.close();
					if(emp.getHotkeyBindingPane() != null) {
						emp.getHotkeyBindingPane().close();
					}
					emp = new EditModePane(this, engine);
				}
				ch.destory();
				setLastSkill(null);
				
				ch = new CraftingHistoryPane(this);

				engine = new Engine(craftsmanship, control, cp, totalDurability, totalProgress, totalQuality, 
						rCraftsmanship, rControl, progressDifference, 
						qualityDifference, seed, m);
				
				// Creates a new engine to restart everything
				lm = engine.getLogManager();
				lm.setViewManager(this);
				
				SkillIcon.setVm(engine, tml, this);
				updateAll();
				ch.display();
				
				closeSubPanes(false);
				
				engine.setEngineStatus(EngineStatus.Replaying);
			} catch (CraftingException e1) {
				postInvalidMessage(e1.es);
			}
		});
		
		int i = 0;
		int j = 0;
		gp.add(craftT, i, j);
		gp.add(controlT, i, j + 1);
		gp.add(CPT, i, j + 2);
		i++;
		
		gp.add(craftTf, i, j);
		gp.add(controlTf, i, j + 1);
		gp.add(CPTf, i, j + 2);
		i++;

		gp.add(totalProgT, i, j);
		gp.add(totalQltyT, i, j + 1);
		gp.add(totalDuraT, i, j + 2);
		i++;

		gp.add(totalProgTf, i, j);
		gp.add(totalQltyTf, i, j + 1);
		gp.add(totalDuraTf, i, j + 2);
		i++;

		gp.add(GCDCb, i, j);
		gp.add(cb, i, j + 2);
		i++;
		
		gp.add(confirm, i, j);
		gp.add(logs, i, j + 1);
		i++;
		
		gp.add(finish, i, j);
		i++;
		
		i++;
		i++;
		gp.add(iconRearr, i, j );
		gp.add(advanced, i, j + 1);
		i++;
		gp.add(loadConfig, i, j);
		gp.add(saveConfig, i, j + 1);
		i++;
		gp.add(loadReplat, i, j);
		gp.add(saveReplay, i, j + 1);
		i++;
		
//		Button b1 = new Button("Test");
//		b1.setOnMouseClicked(e -> {
//			HotkeyBindingPane btp = new HotkeyBindingPane(this);
//		});
//		
//		gp.add(b1, i, j);
//		i++;

		// Draw the edge of the pane
		border.setPrefWidth(REC_WIDTH + EDGE_GENERAL);
		border.add(back, 0, 0);
		border.setBackground(new Background(new BackgroundFill(Color.SILVER, new CornerRadii(10.0), null)));
		
		back.add(gp, 0, 0);
		back.setBackground(new Background(new BackgroundFill(Color.rgb(25,30,37,1.0), new CornerRadii(10.0), null)));
		
		GridPane.setMargin(back, new Insets(EDGE_GENERAL / 2));
		GridPane.setMargin(gp, new Insets(10));
		
		gp.autosize();		
		
		t.add(craftT);
		t.add(controlT);
		t.add(CPT);
		t.add(totalProgT);
		t.add(totalQltyT);
		t.add(totalDuraT);
		
		for(Text tx: t) {
			tx.setFill(Color.WHITE);
		}
		
		return border;
	}
	
	private Node initInfoDisplay() {
		GridPane gp = new GridPane();
		Rectangle rec1 = new Rectangle(REC_WIDTH, 2, Color.rgb(64, 64, 64));
		Rectangle rec2 = new Rectangle(REC_WIDTH, 2, Color.rgb(38, 38, 38));
		
		int i = 0;
		gp.add(initProgressBar(), 0, i++);
		gp.add(rec2, 0, i++);
		gp.add(rec1, 0, i++);
		gp.add(initEffAndBuffDisplay(), 0, i++);
		
		gp.setVgap(0);
		
		return gp;
	}
	
	/**
	 * Initiate the two progress bars and other related information display
	 * @return
	 */
	private Node initProgressBar() {
		GridPane container = new GridPane();
		GridPane left = new GridPane();
		GridPane right = new GridPane();
		AnchorPane progressBar = createBar(Color.DARKGREEN, BAR_WIDTH, BAR_HEIGHT, BAR_EDGE);
		AnchorPane qualityBar = createBar(Color.DARKBLUE, BAR_WIDTH, BAR_HEIGHT, BAR_EDGE);
		Text progressText = new Text(totalProgress + "/" + totalProgress);
		Text qualityText = new Text(totalQuality + "/" + totalQuality);
		ArrayList<Text> t = new ArrayList<Text>();
		
		GridPane lb = new GridPane();
		GridPane rt = new GridPane();
		Text status = new Text("ͨ������");
		status.setFill(Color.WHITE);
		statusDisp = new Circle(10, Color.WHITE);
		
		durabilityText = new Text("�;�:  " + totalDurability + "/" + totalDurability);
		round = new Text("����:  1");
		
		durabilityText.setFont(new Font(20));
		
		
		progText.add(progressText);
		progText.add(qualityText);
		progText.add(status);
		
		container.setAlignment(Pos.CENTER);
		left.setAlignment(Pos.CENTER);
		right.setAlignment(Pos.CENTER);
		
		lb.add(statusDisp, 0, 0);
		lb.add(status, 1, 0);
		
		left.add(durabilityText, 0, 0);
		left.add(round, 0, 1);
		left.add(lb, 0, 2);
		
		rt.add(progressBar, 1, 0);
		rt.add(progressText, 2, 0);
		rt.add(qualityBar, 1, 1);
		rt.add(qualityText, 2, 1);
		
		right.add(rt, 0, 0);
		right.add(initCPDisplay(), 0, 1);
		
		GridPane leftBack = new GridPane();
		leftBack.add(left, 0, 0);
		GridPane.setMargin(left, new Insets(10.0));
		
		leftBack.setBackground(new Background(
				new BackgroundFill(Color.rgb(34, 34, 34), new CornerRadii(10.0, 0, 0, 0, false), null)));
		
		GridPane rightBack = new GridPane();
		rightBack.add(right, 0, 0);
		GridPane.setMargin(right, new Insets(10.0));
		
		rightBack.setBackground(new Background(
				new BackgroundFill(Color.rgb(48, 48, 48), new CornerRadii(0, 10.0, 0, 0, false), null)));
		
		container.add(leftBack, 0, 0);
		container.add(rightBack, 1, 0);
		
		rt.setVgap(10.0);
		rt.setHgap(10.0);
		
		left.setVgap(20.0);
		right.setVgap(10.0);
		
		container.setHgap(20);
				
		t.add(durabilityText);
		t.add(round);
		t.add(progressText);
		t.add(qualityText);
		
		for(Text tx: t) {
			tx.setFill(Color.WHITE);
		}
		
		
		GridPane ap = new GridPane();
		ap.add(container, 0, 0);
		GridPane.setMargin(container, new Insets(0, 10.0, 0, 0));
		
		ap.setBackground(new Background(new BackgroundFill(Color.rgb(48, 48, 48), new CornerRadii(10.0, 10.0, 0, 0, false), null)));
//		container.setBackground(new Background(new BackgroundFill(Color.rgb(48, 48, 48), null, null)));
		
		return ap;
	}
	
	/**
	 * Initiate the CP display bar
	 * @return
	 */
	private Node initCPDisplay() {
		HBox container = new HBox();
		AnchorPane cpBar = createBar(Color.PURPLE, CP_WIDTH, CP_HEIGHT, CP_EDGE);
		Text cpVal = new Text(cp + "/" + cp);
		Text success = new Text("Success!");
		Text cp = new Text("CP");
		ArrayList<Text> t = new ArrayList<Text>();

		
//		success.setFill(Color.rgb(48, 48, 48));
		success.setFont(Font.font(15));
		
		container.setAlignment(Pos.CENTER);
		
		
		progText.add(cpVal);
		progText.add(success);
		bars.get(2).setWidth(CP_WIDTH);
		
		container.getChildren().addAll(cp, cpBar, cpVal, success); //status, statusDisp,
		
		container.setSpacing(30);
		container.setLayoutX(10);
		
		t.add(cpVal);
		t.add(cp);
		
		for(Text tx: t) {
			tx.setFill(Color.WHITE);
		}
		
		success.setFill(Color.rgb(48, 48, 48));
		
		GridPane ap = new GridPane();
		ap.add(container, 0, 0);
		GridPane.setMargin(container, new Insets(10.0));
		
//		ap.setBackground(new Background(new BackgroundFill(Color.RED, null, null)));
//		container.setBackground(new Background(new BackgroundFill(Color.YELLOW, null, null)));
		
		return ap;
	} 
	
	private Node initEffAndBuffDisplay() {
		GridPane container = new GridPane();
		
		container.add(initBuffDisp(), 0, 0);
		container.add(initefficiencyDisp(), 0, 1);
		container.setVgap(10.0);
		
		GridPane ap = new GridPane();
		ap.setMinWidth(REC_WIDTH);
		ap.add(container, 0, 0);
		GridPane.setMargin(container, new Insets(10.0));

		ap.setBackground(new Background(new BackgroundFill(Color.rgb(32, 28, 32), new CornerRadii(0, 0, 10.0, 10.0, false), null)));
//		container.setBackground(new Background(new BackgroundFill(Color.YELLOW, null, null)));
		
		return ap;
	}
	
	/**
	 * Initiate the efficiency display text and previous skill display
	 * @return
	 */
	private Node initefficiencyDisp() {
		HBox container = new HBox();
		Text lastSkillT = new Text("��һ������:  ");
		ArrayList<Text> t = new ArrayList<Text>();
		Rectangle dividerRec = new Rectangle(3, 41);

		lastSkillAp = new AnchorPane();
		efficiencyDisp = new GridPane(); 
		recSkillAp = new AnchorPane();
		
		Text line1 = new Text("   100%Ч���µĽ�չ: " + engine.getBaseProgEff());
		Text line2 = new Text("   100%Ч���µ�Ʒ��: " + engine.getBaseQltyEff());
		
		Text recSkillText = new Text("�Ƽ����ܣ�");
		finalizeText = new Text();
		
		recSkillText.setFill(Color.WHITE);
		finalizeText.setFill(Color.YELLOW);
		
		lastSkillAp.setPrefSize(39.0, 39.0);
		lastSkillAp.setMaxSize(39.0, 39.0);
		
		efficiencyDisp.add(line1, 0, 0);
		efficiencyDisp.add(line2, 0, 1);
		
		efficiencyDisp.setVgap(8.0);
		
//		recSkillAp.setPrefSize(39.0, 39.0);
//		recSkillAp.setMaxSize(39.0, 39.0);
//
//		recSkillAp.setBackground(new Background(new BackgroundImage(
//					new Image(BuffSkill.Muscle_Memory.getAddress(), true), null, null,
//					BackgroundPosition.CENTER, null)));
		
		HBox.setMargin(lastSkillT, new Insets(0, 10.0, 0, 10.0));
		HBox.setMargin(lastSkillAp, new Insets(5.0, 30.0, 5.0, 0));
		HBox.setMargin(efficiencyDisp, new Insets(5.0, 30.0, 5.0, 0));
		
//		HBox.setMargin(recSkillText, new Insets(0, 10.0, 0, 20.0));
//		HBox.setMargin(recSkillAp, new Insets(5.0, 30.0, 5.0, 0));
//
//		HBox.setMargin(finalizeText, new Insets(0, 10.0, 0, 30.0));
		
		dividerRec.setFill(Color.RED);

		container.setMinWidth(REC_WIDTH - 32.0);
		container.setAlignment(Pos.CENTER_LEFT);
		container.getChildren().addAll(lastSkillT, lastSkillAp, efficiencyDisp, 
				dividerRec); // , recSkillText , recSkillAp, finalizeText
		
		t.add(lastSkillT);
		t.add(line1);
		t.add(line2);
		
		for(Text tx: t) {
			tx.setFill(Color.WHITE);
		}
		
//		container.setBackground(new Background(new BackgroundFill(Color.RED, null, null)));
		GridPane ap = new GridPane();
		ap.add(container, 0, 0);
		GridPane.setMargin(container, new Insets(2.0));
		
		ap.setBackground(new Background(
				new BackgroundFill(Color.rgb(0, 195, 249), new CornerRadii(5.0), null)));
		container.setBackground(new Background(
				new BackgroundFill(Color.rgb(30, 24, 30), new CornerRadii(5.0), null)));
		
		return ap;
	}
	
	/**
	 * Initiate the buffs display bar
	 * @return
	 */
	private Node initBuffDisp() {
		Text buffText = new Text("  Buff:");
		buffContainer = new HBox(10);
		buffContainer.setPrefHeight(32.0);
		
		buffContainer.getChildren().add(buffText);
		
		buffContainer.setMinHeight(40.0);
		
		buffText.setFont(Font.font(15.0));
		buffText.setFill(Color.WHITE);
		
		return buffContainer;
	}
	
	/**
	 * Initiate the skill icons display
	 * @return
	 */
	private Node initSkills() {
		GridPane skillContainer = new GridPane();
		AnchorPane border = new AnchorPane();	
		
		skillDescription = new Text(" ");
		iconContainer = new GridPane();
		
		skillContainer.setVgap(5);
		skillContainer.add(skillDescription, 0, 1);
//		GridPane.setHgrow(skillDescription, Priority.ALWAYS);
//		GridPane.setVgrow(skillDescription, Priority.ALWAYS);
		
		
		skillContainer.add(iconContainer, 0, 2);
				
		skillDescription.setFill(Color.WHITE);
		iconContainer.setHgap(5);
		
		int i = 2; // Makes it easier to code (easier to copy and paste)
		createSkillList(progressSkills, getIconContainer(), i++);
		createSkillList(qualitySkills, getIconContainer(), i++);
		createSkillList(buffSkills, getIconContainer(), i++);
		createSkillList(recoverySkills, getIconContainer(), i++);
		createSkillList(otherSkills, getIconContainer(), i++);
		
		border.getChildren().add(skillContainer);
		border.setPrefSize(REC_WIDTH + EDGE_GENERAL, SKILL_HEIGHT + EDGE_GENERAL);
		skillContainer.setPrefSize(REC_WIDTH, SKILL_HEIGHT);
		
		border.setBackground(new Background(
				new BackgroundFill(Color.WHITE, new CornerRadii(5.0), null)));
		skillContainer.setBackground(new Background(
				new BackgroundFill(Color.rgb(35, 35, 35), new CornerRadii(5.0), null)));
		
		
		
		AnchorPane.setLeftAnchor(skillContainer, EDGE_GENERAL / 2);
		AnchorPane.setTopAnchor(skillContainer, EDGE_GENERAL / 2);
		
		for(Node n: skillContainer.getChildren()) {
			GridPane.setMargin(n, new Insets(0, 0, 0, 10));
		}
		
		return border;
	}
	
	/**
	 * Initiate the skill icons display
	 * @param skl the list that store skills that needed to be displayer
	 * @param gp  the GridPane that stores all these icons
	 * @param i   the line which these skills are located
	 */
	private void createSkillList(List<Skill> skl, GridPane gp, int i) {
		int j = 1;
		for(Skill s: skl) {			
			SkillIcon si = new SkillIcon(s, tml, this);
			skillIcons.add(si);
			
			gp.add(si, j, i);
			j++;
		}
		
		// Fill the rest with empty ones
		for(; j <= 12; j++) { 
			SkillIcon si = new SkillIcon(null, tml, this);
			skillIcons.add(si);
			gp.add(si, j, i);
		}
		
		SkillIcon.setVm(engine, tml, this);

		return;
	}
	
	/**
	 * The main method that draws the bar
	 * @param c the color of the bar (Color class is from javafx)
	 * @param width the width of the bar
	 * @param height the height of the bar
	 * @param paneEdge the edge thickness of the bar
	 * @return
	 */
	private AnchorPane createBar(Color c, double width, double height, double paneEdge) {
		AnchorPane bar = new AnchorPane();
		Rectangle edgeR = new Rectangle(width + 2 * paneEdge, height + 2 * paneEdge, Color.SILVER);
		Rectangle fill = new Rectangle(width, height, Color.rgb(32, 32, 32));
		Rectangle progress = new Rectangle(0, height, c);
		
		edgeR.setArcWidth(15.0);
		edgeR.setArcHeight(15.0);
		fill.setArcWidth(15.0);
		fill.setArcHeight(15.0);
		progress.setArcWidth(15.0);
		progress.setArcHeight(15.0);
		
		bars.add(progress);
		
		bar.getChildren().add(edgeR);
		bar.getChildren().add(fill);
		bar.getChildren().add(progress);
		
		AnchorPane.setTopAnchor(fill, (double)BAR_EDGE);
		AnchorPane.setTopAnchor(progress, (double)BAR_EDGE);
		AnchorPane.setLeftAnchor(fill, (double)BAR_EDGE);
		AnchorPane.setLeftAnchor(progress, (double)BAR_EDGE);
			
		return bar;
	}
	
	private void closeSubPanes(boolean closeDisplayPane) {
		if(asp != null) {
			asp.close();
			if(asp.getDp() != null) {
				asp.getDp().close();
			}
		}
		if(emp != null) {
			emp.close();
			if(emp.getHotkeyBindingPane() != null) {
				emp.getHotkeyBindingPane().close();
			}
		}
		if(closeDisplayPane) {
			if(ch != null) {
				ch.close();
			}
		}
	}
	
	/**
	 * Updates all the displays
	 */
	public void updateAll() {
		node = lm.getPresentNode();
//		updateRecSkill();
		updateProgress();
		updateQuality();
		updateCP();
		updateDur();
		updateEffDisp();
		updateSuccess();
		updateBuffDIsp();
		updateStatus();
		updateLastSkill();
		updateSkillCP();
	}
	
//	public void updateRecSkill() {
//		recSkillAp.setBackground(new Background(new BackgroundImage(
//				new Image(engine.getRecSkill().getAddress(), true), null, null,
//				BackgroundPosition.CENTER, null)));
//
//		int i = engine.getFinalizeSequence();
//		if(i != 0) {
//			if(i == 1) {
//				finalizeText.setText("�ȶ��������β");
//			}
//			else if(i == 1) {
//				finalizeText.setText("�ȶ���������β");
//			}
//			else if(i == 1) {
//				finalizeText.setText("˫�µرȶ�����β");
//			}
//		}
//	}
	
	public void updateProgress() {
		progText.get(0).setText(engine.getPresentProgress() + "/" + engine.getTotalProgress());
		if(engine.getPresentProgress()>=engine.getTotalProgress()) {
			bars.get(0).setWidth(BAR_WIDTH);
		} else {
			bars.get(0).setWidth((double)engine.getPresentProgress()/engine.getTotalProgress()*BAR_WIDTH);
		}	
	}
	
	public void updateQuality() {
		progText.get(1).setText(engine.getPresentQuality() + "/" + engine.getTotalQuality());
		if(engine.getPresentQuality()>=engine.getTotalQuality()) {
			bars.get(1).setWidth(BAR_WIDTH);
		} else {
			bars.get(1).setWidth((double)engine.getPresentQuality()/engine.getTotalQuality()*BAR_WIDTH);
		}
	}
	
	public void updateCP() {
		progText.get(3).setText(engine.getPresentCP() + "/" + engine.getTotalCP());
		if(engine.getPresentCP()>=engine.getTotalCP()) {
			bars.get(2).setWidth(CP_WIDTH);
		} else {
			bars.get(2).setWidth((double)engine.getPresentCP()/engine.getTotalCP()*CP_WIDTH);
		}
	}
	
	public void updateDur() {
		durabilityText.setText("�;�:  " + engine.getPresentDurability()+ "/" + engine.getTotalDurability());
		round.setText("����:  " + engine.getRound());;
		
		if(engine.getPresentDurability() <= 10) {
			durabilityText.setFill(Color.RED);
		} else if(engine.getPresentDurability() <= 20) {
			durabilityText.setFill(Color.ORANGE);
		} else {
			durabilityText.setFill(Color.WHITE);
		}
	}
	
	public void updateEffDisp() {
		((Text)efficiencyDisp.getChildren().get(0)).setText("  100%Ч���µĽ�չ: " + engine.getBaseProgEff());
		((Text)efficiencyDisp.getChildren().get(1)).setText("  100%Ч���µ�Ʒ��: " + engine.getBaseQltyEff());

	}
	
	public void updateSuccess() {
		Text t = progText.get(4);
		
		if(engine.isSkillSuccess()) {
			t.setText("Success!");
			t.setFill(Color.GREEN);
		} else {
			t.setText("Fail...");
			t.setFill(Color.RED);
		}
		
	}
	
	public void updateStatus() {
		progText.get(2).setText(engine.getCraftingStatus().getName());
		progText.get(2).setFill(engine.getCraftingStatus().getFxColor());
		statusDisp.setFill(engine.getCraftingStatus().getFxColor());
	}
	
	public void updateBuffDIsp() {
		Text buffText = new Text("  Buff:");
		
		buffText.setFill(Color.WHITE);
		
		buffContainer.getChildren().clear();
		buffContainer.getChildren().add(buffText);
		
		for(ActiveBuff ab: engine.getActiveBuffs()) {			
			AnchorPane ap = new AnchorPane();
			ImageView iv = null;
			if(ab.buff == Buff.inner_quiet) {
				String add = "/icons/Inner_Quiet_Icon/Inner_Quiet_" + ab.getRemaining() + ".png";
				iv = new ImageView(new Image(add, true));
			} else {
				iv = new ImageView(new Image(ab.buff.getAddress(), true));
			}
			Text remaining = new Text(Integer.toString(ab.getRemaining()));
			remaining.setFill(Color.WHITE);
			
			ap.getChildren().add(iv);
			ap.getChildren().add(remaining);

			buffContainer.getChildren().add(ap);
			HBox.setMargin(ap, new Insets(5.0, 0.0, 5.0, 0.0));
		}
	}
	
	public void updateLastSkill() {
		if(getLastSkill() == null) {
			lastSkillAp.setBackground(Background.EMPTY);
		} else {
			lastSkillAp.setBackground(new Background(new BackgroundImage(
					new Image(getLastSkill().getAddress(), true), null, null, 
					BackgroundPosition.CENTER, null)));
		}
		
	}
	
	private void updateSkillCP() {
		Iterator<Node> iter = iconContainer.getChildren().iterator();
		while(iter.hasNext()) {
			SkillIcon si = (SkillIcon)iter.next();
			if(si.getSkill()!=null) {
				int i = si.getSkill().getCPCost();
				i = (engine.getCraftingStatus() == CraftingStatus.Pliant ? (i+1)/2 : i);
				if(i!=0) {
					si.setCostText(Integer.toString(i));
				} else {
					si.setCostText(" ");
				}
			}
		}
	}
	
	/**
	 * pop up the crafting finished message box
	 * @param es
	 */
	public void postFinishMessage(ExceptionStatus es) {
		
		// Update the CP cost (otherwise it might be halved)
		Iterator<Node> iter = iconContainer.getChildren().iterator();
		while(iter.hasNext()) {
			SkillIcon si = (SkillIcon)iter.next();
			if(si.getSkill()!=null) {
				int i = si.getSkill().getCPCost();
				if(i!=0) {
					si.setCostText(Integer.toString(i));
				} else {
					si.setCostText(" ");
				}
			}
		}
		
		Alert al = new Alert(AlertType.INFORMATION);
		GridPane gp = new GridPane();
		Text DebugMode = new Text(usedDebug ? "ʹ�ù�Debug" : "");
		Text GCDMode = new Text("GCD: " + (getHasGCD() ? "����" : "�ر�"));
		Text runTime = new Text("����ʱ:  " + Double.toString(engine.getRuntime()) + "��");
		Text val = new Text("�ղؼ�ֵ:  " + engine.getPresentQuality() / 10);
		
		updateAll(); // update before taking summary
		
		engine.setEngineStatus(EngineStatus.Pending);;
		
		lm.setFinishInfo(usedDebug, hasGCD, engine.getRuntime(), 
				engine.getPresentQuality() / 10, engine.SPCalc(), engine.getRound());
		
		al.setTitle(es == ExceptionStatus.Craft_Failed ? "����ʧ��...." : "�����ɹ���");
		al.setHeaderText(es == ExceptionStatus.Craft_Failed ? "��ѽ������ʧ����...." : "��ϲ�������ɹ���");
		
		int i = 0;
		if(usedDebug) {
			gp.add(DebugMode, 0, i++);
		}
		gp.add(GCDMode, 0, i++);
		gp.add(runTime, 0, i++);
		gp.add(val, 0, i++);
		
		if(es == ExceptionStatus.Craft_Success) {		
			Text SP = new Text("���ɵ���:  " + engine.SPCalc());
			gp.add(SP, 0, i++);	
		}
		
		al.getDialogPane().setExpandableContent(gp);
		al.getDialogPane().setExpanded(true);

		al.showAndWait();
	}
	
	/**
	 *  Pop up the invalid action message box
	 * @param es stores the error message
	 */
	public void postInvalidMessage(ExceptionStatus es) {
		Alert al = new Alert(AlertType.WARNING);
		
		al.setTitle("�޷�ʹ��");
		al.setContentText(es.getMessage());
		
		al.showAndWait();
	}
	
	/**
	 *  Pop up unexpected error message (hasn't triggered yet
	 */
	public void postUnexpectedMessage() {
		Alert al = new Alert(AlertType.WARNING);
		
		al.setTitle("δ֪����");
		al.setContentText("������ô������...");
		
		al.showAndWait();
	}
	
	/**
	 * Main function to export the logs, which is stored in engine
	 */
	public void exportLogs() {
		Alert al = new Alert(AlertType.INFORMATION);
		GridPane container = new GridPane();
		Text title = new Text("��־");
		TextArea logsOutput = new TextArea();
		
		logsOutput.setEditable(false);
		logsOutput.setWrapText(false);
		
		lm.createLogs();
		
		for(String s: lm.exportLogs()) {
			logsOutput.setText(logsOutput.getText() + "\n" + s);
		}
		
		GridPane.setVgrow(logsOutput, Priority.ALWAYS);
		GridPane.setHgrow(logsOutput, Priority.ALWAYS);
		
		al.setTitle("����Ϊ��־���");
		al.setHeaderText(null);
		
		container.setMaxWidth(Double.MAX_VALUE);
		container.add(title, 0, 0);
		container.add(logsOutput, 0, 1);
		
		al.getDialogPane().setExpandableContent(container);
		al.getDialogPane().setExpanded(true);
		
		al.showAndWait();
	}
	
	public void updateProperties() {
		inputTf.get(0).setText(Integer.toString(craftsmanship));
		inputTf.get(1).setText(Integer.toString(control));
		inputTf.get(2).setText(Integer.toString(cp));
	}
	
	public String exportHotkeyBinding() {
		String res = "";
		for(SkillIcon si: skillIcons) {	
			if(si.getKey() == null && si.getMod() == null) {
				res += "XX";
			} else {
				res += (si.getKey() + si.getMod());
			}
		}
		return res;
	}
	
	public void importHotkeyBinding(String s) throws IOException {
		if(s.length() != 120) {
			throw new IOException();
		}
		
		for(int i = 0; i < 60; i++) {
			SkillIcon si = skillIcons.get(i);
			String rawKey = s.substring(i * 2, i * 2 + 1);

			String rawMod = s.substring(i * 2 + 1, i * 2 + 2);
			if(rawKey.equals("X") && rawMod.equals("X") ) {
				si.setKeyCombination(null, null);
			} else if(rawMod.equals("s")) {
				si.setKeyCombination(rawKey, "SHIFT");
			} else if(rawMod.equals("c")) {
				si.setKeyCombination(rawKey, "CONTROL");
			} else if(rawMod.equals("a")) {
				si.setKeyCombination(rawKey, "ALT");
			} else if(rawMod.equals("n")) {
				si.setKeyCombination(rawKey, null);
			} else {
				importHotkeyError();;
			}
		}
	}
	
	private void importHotkeyError() throws IOException {
		for(SkillIcon si: skillIcons) {
			si.setKeyCombination(null, null);
		}
		throw new IOException();
	}
	
	public void importPlayerData(String craft, String control, String cp) {
		inputTf.get(0).setText(craft);
		inputTf.get(1).setText(control);
		inputTf.get(2).setText(cp);
	}
	
	// == getters and setters ==
	public Stage getStage() {
		return stage;
	}
	
	public ArrayList<SkillIcon> getSkillIcons() {
		return skillIcons;
	}
	
	public EditModePane getEditModePane() {
		return emp;
	}
	
	public Text getSkillDescription() {
		return skillDescription;
	}

	public void setSkillDescription(Text skillDescription) {
		this.skillDescription = skillDescription;
	}
	
	public GridPane getIconContainer() {
		return iconContainer;
	}

	public void setIconContainer(GridPane iconContainer) {
		this.iconContainer = iconContainer;
	}
	
	public ArrayList<TextField> getInputTf()
	{
		return inputTf;
	}
	
	public CraftingHistoryPane getCraftingHistoryPane() {
		return ch;
	}
}
