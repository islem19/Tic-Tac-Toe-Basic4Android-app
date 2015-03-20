package XOX.Android;

import anywheresoftware.b4a.B4AMenuItem;
import android.app.Activity;
import android.os.Bundle;
import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BALayout;
import anywheresoftware.b4a.B4AActivity;
import anywheresoftware.b4a.ObjectWrapper;
import anywheresoftware.b4a.objects.ActivityWrapper;
import java.lang.reflect.InvocationTargetException;
import anywheresoftware.b4a.B4AUncaughtException;
import anywheresoftware.b4a.debug.*;
import java.lang.ref.WeakReference;

public class main extends Activity implements B4AActivity{
	public static main mostCurrent;
	static boolean afterFirstLayout;
	static boolean isFirst = true;
    private static boolean processGlobalsRun = false;
	BALayout layout;
	public static BA processBA;
	BA activityBA;
    ActivityWrapper _activity;
    java.util.ArrayList<B4AMenuItem> menuItems;
	public static final boolean fullScreen = true;
	public static final boolean includeTitle = false;
    public static WeakReference<Activity> previousOne;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFirst) {
			processBA = new BA(this.getApplicationContext(), null, null, "XOX.Android", "XOX.Android.main");
			processBA.loadHtSubs(this.getClass());
	        float deviceScale = getApplicationContext().getResources().getDisplayMetrics().density;
	        BALayout.setDeviceScale(deviceScale);
            
		}
		else if (previousOne != null) {
			Activity p = previousOne.get();
			if (p != null && p != this) {
                BA.LogInfo("Killing previous instance (main).");
				p.finish();
			}
		}
		if (!includeTitle) {
        	this.getWindow().requestFeature(android.view.Window.FEATURE_NO_TITLE);
        }
        if (fullScreen) {
        	getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,   
        			android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
		mostCurrent = this;
        processBA.sharedProcessBA.activityBA = null;
		layout = new BALayout(this);
		setContentView(layout);
		afterFirstLayout = false;
		BA.handler.postDelayed(new WaitForLayout(), 5);

	}
	private static class WaitForLayout implements Runnable {
		public void run() {
			if (afterFirstLayout)
				return;
			if (mostCurrent == null)
				return;
            
			if (mostCurrent.layout.getWidth() == 0) {
				BA.handler.postDelayed(this, 5);
				return;
			}
			mostCurrent.layout.getLayoutParams().height = mostCurrent.layout.getHeight();
			mostCurrent.layout.getLayoutParams().width = mostCurrent.layout.getWidth();
			afterFirstLayout = true;
			mostCurrent.afterFirstLayout();
		}
	}
	private void afterFirstLayout() {
        if (this != mostCurrent)
			return;
		activityBA = new BA(this, layout, processBA, "XOX.Android", "XOX.Android.main");
        
        processBA.sharedProcessBA.activityBA = new java.lang.ref.WeakReference<BA>(activityBA);
        anywheresoftware.b4a.objects.ViewWrapper.lastId = 0;
        _activity = new ActivityWrapper(activityBA, "activity");
        anywheresoftware.b4a.Msgbox.isDismissing = false;
        if (BA.isShellModeRuntimeCheck(processBA)) {
			if (isFirst)
				processBA.raiseEvent2(null, true, "SHELL", false);
			processBA.raiseEvent2(null, true, "CREATE", true, "XOX.Android.main", processBA, activityBA, _activity, anywheresoftware.b4a.keywords.Common.Density);
			_activity.reinitializeForShell(activityBA, "activity");
		}
        initializeProcessGlobals();		
        initializeGlobals();
        
        BA.LogInfo("** Activity (main) Create, isFirst = " + isFirst + " **");
        processBA.raiseEvent2(null, true, "activity_create", false, isFirst);
		isFirst = false;
		if (this != mostCurrent)
			return;
        processBA.setActivityPaused(false);
        BA.LogInfo("** Activity (main) Resume **");
        processBA.raiseEvent(null, "activity_resume");
        if (android.os.Build.VERSION.SDK_INT >= 11) {
			try {
				android.app.Activity.class.getMethod("invalidateOptionsMenu").invoke(this,(Object[]) null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	public void addMenuItem(B4AMenuItem item) {
		if (menuItems == null)
			menuItems = new java.util.ArrayList<B4AMenuItem>();
		menuItems.add(item);
	}
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (menuItems == null)
			return false;
		for (B4AMenuItem bmi : menuItems) {
			android.view.MenuItem mi = menu.add(bmi.title);
			if (bmi.drawable != null)
				mi.setIcon(bmi.drawable);
            if (android.os.Build.VERSION.SDK_INT >= 11) {
				try {
                    if (bmi.addToBar) {
				        android.view.MenuItem.class.getMethod("setShowAsAction", int.class).invoke(mi, 1);
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			mi.setOnMenuItemClickListener(new B4AMenuItemsClickListener(bmi.eventName.toLowerCase(BA.cul)));
		}
		return true;
	}
    public void onWindowFocusChanged(boolean hasFocus) {
       super.onWindowFocusChanged(hasFocus);
       if (processBA.subExists("activity_windowfocuschanged"))
           processBA.raiseEvent2(null, true, "activity_windowfocuschanged", false, hasFocus);
    }
	private class B4AMenuItemsClickListener implements android.view.MenuItem.OnMenuItemClickListener {
		private final String eventName;
		public B4AMenuItemsClickListener(String eventName) {
			this.eventName = eventName;
		}
		public boolean onMenuItemClick(android.view.MenuItem item) {
			processBA.raiseEvent(item.getTitle(), eventName + "_click");
			return true;
		}
	}
    public static Class<?> getObject() {
		return main.class;
	}
    private Boolean onKeySubExist = null;
    private Boolean onKeyUpSubExist = null;
	@Override
	public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
		if (onKeySubExist == null)
			onKeySubExist = processBA.subExists("activity_keypress");
		if (onKeySubExist) {
			if (keyCode == anywheresoftware.b4a.keywords.constants.KeyCodes.KEYCODE_BACK &&
					android.os.Build.VERSION.SDK_INT >= 18) {
				HandleKeyDelayed hk = new HandleKeyDelayed();
				hk.kc = keyCode;
				BA.handler.post(hk);
				return true;
			}
			else {
				boolean res = new HandleKeyDelayed().runDirectly(keyCode);
				if (res)
					return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	private class HandleKeyDelayed implements Runnable {
		int kc;
		public void run() {
			runDirectly(kc);
		}
		public boolean runDirectly(int keyCode) {
			Boolean res =  (Boolean)processBA.raiseEvent2(_activity, false, "activity_keypress", false, keyCode);
			if (res == null || res == true) {
                return true;
            }
            else if (keyCode == anywheresoftware.b4a.keywords.constants.KeyCodes.KEYCODE_BACK) {
				finish();
				return true;
			}
            return false;
		}
		
	}
    @Override
	public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
		if (onKeyUpSubExist == null)
			onKeyUpSubExist = processBA.subExists("activity_keyup");
		if (onKeyUpSubExist) {
			Boolean res =  (Boolean)processBA.raiseEvent2(_activity, false, "activity_keyup", false, keyCode);
			if (res == null || res == true)
				return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	@Override
	public void onNewIntent(android.content.Intent intent) {
		this.setIntent(intent);
	}
    @Override 
	public void onPause() {
		super.onPause();
        if (_activity == null) //workaround for emulator bug (Issue 2423)
            return;
		anywheresoftware.b4a.Msgbox.dismiss(true);
        BA.LogInfo("** Activity (main) Pause, UserClosed = " + activityBA.activity.isFinishing() + " **");
        processBA.raiseEvent2(_activity, true, "activity_pause", false, activityBA.activity.isFinishing());		
        processBA.setActivityPaused(true);
        mostCurrent = null;
        if (!activityBA.activity.isFinishing())
			previousOne = new WeakReference<Activity>(this);
        anywheresoftware.b4a.Msgbox.isDismissing = false;
	}

	@Override
	public void onDestroy() {
        super.onDestroy();
		previousOne = null;
	}
    @Override 
	public void onResume() {
		super.onResume();
        mostCurrent = this;
        anywheresoftware.b4a.Msgbox.isDismissing = false;
        if (activityBA != null) { //will be null during activity create (which waits for AfterLayout).
        	ResumeMessage rm = new ResumeMessage(mostCurrent);
        	BA.handler.post(rm);
        }
	}
    private static class ResumeMessage implements Runnable {
    	private final WeakReference<Activity> activity;
    	public ResumeMessage(Activity activity) {
    		this.activity = new WeakReference<Activity>(activity);
    	}
		public void run() {
			if (mostCurrent == null || mostCurrent != activity.get())
				return;
			processBA.setActivityPaused(false);
            BA.LogInfo("** Activity (main) Resume **");
		    processBA.raiseEvent(mostCurrent._activity, "activity_resume", (Object[])null);
		}
    }
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
	      android.content.Intent data) {
		processBA.onActivityResult(requestCode, resultCode, data);
	}
	private static void initializeGlobals() {
		processBA.raiseEvent2(null, true, "globals", false, (Object[])null);
	}

public anywheresoftware.b4a.keywords.Common __c = null;
public static String[][] _buttonstext = null;
public anywheresoftware.b4a.objects.PanelWrapper _main_pnl = null;
public anywheresoftware.b4a.objects.LabelWrapper _lbl = null;
public anywheresoftware.b4a.objects.LabelWrapper _player_lbl = null;
public anywheresoftware.b4a.objects.ButtonWrapper[][] _buttons = null;
public static int _j = 0;
public static int _i = 0;

public static boolean isAnyActivityVisible() {
    boolean vis = false;
vis = vis | (main.mostCurrent != null);
return vis;}
public static String  _activity_create(boolean _firsttime) throws Exception{
int _width = 0;
int _offsetx = 0;
int _offsety = 0;
anywheresoftware.b4a.objects.ButtonWrapper _b = null;
 //BA.debugLineNum = 33;BA.debugLine="Sub Activity_Create(FirstTime As Boolean)";
 //BA.debugLineNum = 35;BA.debugLine="Activity.LoadLayout(\"Main\")";
mostCurrent._activity.LoadLayout("Main",mostCurrent.activityBA);
 //BA.debugLineNum = 36;BA.debugLine="main_pnl.SetLayout(0,0,100%x,100%y)";
mostCurrent._main_pnl.SetLayout((int) (0),(int) (0),anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (100),mostCurrent.activityBA),anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (100),mostCurrent.activityBA));
 //BA.debugLineNum = 37;BA.debugLine="main_pnl.Color = Colors.Black";
mostCurrent._main_pnl.setColor(anywheresoftware.b4a.keywords.Common.Colors.Black);
 //BA.debugLineNum = 38;BA.debugLine="lbl.Text = \"Current Player :\"";
mostCurrent._lbl.setText((Object)("Current Player :"));
 //BA.debugLineNum = 39;BA.debugLine="player_lbl.Text =\"X\"";
mostCurrent._player_lbl.setText((Object)("X"));
 //BA.debugLineNum = 41;BA.debugLine="Activity.AddMenuItem(\"Restart\",\"restart\")";
mostCurrent._activity.AddMenuItem("Restart","restart");
 //BA.debugLineNum = 45;BA.debugLine="Dim width, offsetX, offsetY As Int";
_width = 0;
_offsetx = 0;
_offsety = 0;
 //BA.debugLineNum = 46;BA.debugLine="width = 70dip";
_width = anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (70));
 //BA.debugLineNum = 47;BA.debugLine="offsetX = ((100%x - width * 10 - 2dip * 9) /2  ) + 230dip";
_offsetx = (int) (((anywheresoftware.b4a.keywords.Common.PerXToCurrent((float) (100),mostCurrent.activityBA)-_width*10-anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (2))*9)/(double)2)+anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (230)));
 //BA.debugLineNum = 48;BA.debugLine="offsetY = (100%y - width * 3 - 2dip * 2) - 150dip";
_offsety = (int) ((anywheresoftware.b4a.keywords.Common.PerYToCurrent((float) (100),mostCurrent.activityBA)-_width*3-anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (2))*2)-anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (150)));
 //BA.debugLineNum = 50;BA.debugLine="For i = 0 To 2";
{
final int step21 = 1;
final int limit21 = (int) (2);
for (_i = (int) (0); (step21 > 0 && _i <= limit21) || (step21 < 0 && _i >= limit21); _i = ((int)(0 + _i + step21))) {
 //BA.debugLineNum = 51;BA.debugLine="For j = 0 To 2";
{
final int step22 = 1;
final int limit22 = (int) (2);
for (_j = (int) (0); (step22 > 0 && _j <= limit22) || (step22 < 0 && _j >= limit22); _j = ((int)(0 + _j + step22))) {
 //BA.debugLineNum = 52;BA.debugLine="Dim b As Button";
_b = new anywheresoftware.b4a.objects.ButtonWrapper();
 //BA.debugLineNum = 53;BA.debugLine="b.Initialize(\"button\") 'All buttons share the same event sub";
_b.Initialize(mostCurrent.activityBA,"button");
 //BA.debugLineNum = 54;BA.debugLine="b.TextSize = 25";
_b.setTextSize((float) (25));
 //BA.debugLineNum = 56;BA.debugLine="Activity.AddView(b,offsetX + i * (width + 20dip), offsetY + j * (width + 20dip), width, width)";
mostCurrent._activity.AddView((android.view.View)(_b.getObject()),(int) (_offsetx+_i*(_width+anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (20)))),(int) (_offsety+_j*(_width+anywheresoftware.b4a.keywords.Common.DipToCurrent((int) (20)))),_width,_width);
 //BA.debugLineNum = 57;BA.debugLine="buttons(i, j) = b 'store a reference to this view";
mostCurrent._buttons[_i][_j] = _b;
 }
};
 }
};
 //BA.debugLineNum = 61;BA.debugLine="End Sub";
return "";
}
public static String  _activity_pause(boolean _userclosed) throws Exception{
 //BA.debugLineNum = 154;BA.debugLine="Sub Activity_Pause (UserClosed As Boolean)";
 //BA.debugLineNum = 155;BA.debugLine="If UserClosed Then";
if (_userclosed) { 
 //BA.debugLineNum = 157;BA.debugLine="NewGame";
_newgame();
 };
 //BA.debugLineNum = 160;BA.debugLine="For i = 0 To 2";
{
final int step108 = 1;
final int limit108 = (int) (2);
for (_i = (int) (0); (step108 > 0 && _i <= limit108) || (step108 < 0 && _i >= limit108); _i = ((int)(0 + _i + step108))) {
 //BA.debugLineNum = 161;BA.debugLine="For j = 0 To 2";
{
final int step109 = 1;
final int limit109 = (int) (2);
for (_j = (int) (0); (step109 > 0 && _j <= limit109) || (step109 < 0 && _j >= limit109); _j = ((int)(0 + _j + step109))) {
 //BA.debugLineNum = 162;BA.debugLine="ButtonsText(i,j) = buttons(i,j).Text";
mostCurrent._buttonstext[_i][_j] = mostCurrent._buttons[_i][_j].getText();
 }
};
 }
};
 //BA.debugLineNum = 165;BA.debugLine="End Sub";
return "";
}
public static String  _activity_resume() throws Exception{
 //BA.debugLineNum = 145;BA.debugLine="Sub Activity_Resume";
 //BA.debugLineNum = 146;BA.debugLine="For i = 0 To 2";
{
final int step98 = 1;
final int limit98 = (int) (2);
for (_i = (int) (0); (step98 > 0 && _i <= limit98) || (step98 < 0 && _i >= limit98); _i = ((int)(0 + _i + step98))) {
 //BA.debugLineNum = 147;BA.debugLine="For j = 0 To 2";
{
final int step99 = 1;
final int limit99 = (int) (2);
for (_j = (int) (0); (step99 > 0 && _j <= limit99) || (step99 < 0 && _j >= limit99); _j = ((int)(0 + _j + step99))) {
 //BA.debugLineNum = 148;BA.debugLine="buttons(i, j).Text = ButtonsText(i, j)";
mostCurrent._buttons[_i][_j].setText((Object)(mostCurrent._buttonstext[_i][_j]));
 }
};
 }
};
 //BA.debugLineNum = 152;BA.debugLine="End Sub";
return "";
}
public static String  _button_click() throws Exception{
anywheresoftware.b4a.objects.ButtonWrapper _b = null;
boolean _freecellleft = false;
int _x = 0;
int _y = 0;
 //BA.debugLineNum = 70;BA.debugLine="Sub button_Click";
 //BA.debugLineNum = 71;BA.debugLine="Dim b As Button";
_b = new anywheresoftware.b4a.objects.ButtonWrapper();
 //BA.debugLineNum = 72;BA.debugLine="b = Sender";
_b.setObject((android.widget.Button)(anywheresoftware.b4a.keywords.Common.Sender(mostCurrent.activityBA)));
 //BA.debugLineNum = 73;BA.debugLine="If b.Text <> \"\" Then Return";
if ((_b.getText()).equals("") == false) { 
if (true) return "";};
 //BA.debugLineNum = 74;BA.debugLine="b.Text = player_lbl.Text";
_b.setText((Object)(mostCurrent._player_lbl.getText()));
 //BA.debugLineNum = 76;BA.debugLine="If player_lbl.Text =\"X\" Then";
if ((mostCurrent._player_lbl.getText()).equals("X")) { 
 //BA.debugLineNum = 77;BA.debugLine="player_lbl.Text =\"O\"";
mostCurrent._player_lbl.setText((Object)("O"));
 }else {
 //BA.debugLineNum = 79;BA.debugLine="player_lbl.Text=\"X\"";
mostCurrent._player_lbl.setText((Object)("X"));
 };
 //BA.debugLineNum = 82;BA.debugLine="If CheckIfWin( b.Text ) Then";
if (_checkifwin(_b.getText())) { 
 //BA.debugLineNum = 84;BA.debugLine="Msgbox(\"The winner is: \" & b.Text, \"\")";
anywheresoftware.b4a.keywords.Common.Msgbox("The winner is: "+_b.getText(),"",mostCurrent.activityBA);
 //BA.debugLineNum = 85;BA.debugLine="NewGame";
_newgame();
 //BA.debugLineNum = 86;BA.debugLine="Return";
if (true) return "";
 };
 //BA.debugLineNum = 89;BA.debugLine="Dim freeCellLeft As Boolean";
_freecellleft = false;
 //BA.debugLineNum = 90;BA.debugLine="freeCellLeft = False";
_freecellleft = anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 91;BA.debugLine="For x = 0 To 2";
{
final int step51 = 1;
final int limit51 = (int) (2);
for (_x = (int) (0); (step51 > 0 && _x <= limit51) || (step51 < 0 && _x >= limit51); _x = ((int)(0 + _x + step51))) {
 //BA.debugLineNum = 92;BA.debugLine="For y = 0 To 2";
{
final int step52 = 1;
final int limit52 = (int) (2);
for (_y = (int) (0); (step52 > 0 && _y <= limit52) || (step52 < 0 && _y >= limit52); _y = ((int)(0 + _y + step52))) {
 //BA.debugLineNum = 93;BA.debugLine="freeCellLeft = freeCellLeft OR buttons(x, y).Text = \"\"";
_freecellleft = _freecellleft || (mostCurrent._buttons[_x][_y].getText()).equals("");
 }
};
 }
};
 //BA.debugLineNum = 96;BA.debugLine="If freeCellLeft = False Then";
if (_freecellleft==anywheresoftware.b4a.keywords.Common.False) { 
 //BA.debugLineNum = 97;BA.debugLine="Msgbox(\"Both lost!\", \"\")";
anywheresoftware.b4a.keywords.Common.Msgbox("Both lost!","",mostCurrent.activityBA);
 //BA.debugLineNum = 98;BA.debugLine="NewGame";
_newgame();
 };
 //BA.debugLineNum = 100;BA.debugLine="End Sub";
return "";
}
public static boolean  _checkifwin(String _player) throws Exception{
boolean _found = false;
int _x = 0;
int _y = 0;
 //BA.debugLineNum = 112;BA.debugLine="Sub CheckIfWin (Player As String) As Boolean";
 //BA.debugLineNum = 114;BA.debugLine="Dim found As Boolean";
_found = false;
 //BA.debugLineNum = 115;BA.debugLine="For x = 0 To 2";
{
final int step71 = 1;
final int limit71 = (int) (2);
for (_x = (int) (0); (step71 > 0 && _x <= limit71) || (step71 < 0 && _x >= limit71); _x = ((int)(0 + _x + step71))) {
 //BA.debugLineNum = 116;BA.debugLine="found = True";
_found = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 117;BA.debugLine="For y = 0 To 2";
{
final int step73 = 1;
final int limit73 = (int) (2);
for (_y = (int) (0); (step73 > 0 && _y <= limit73) || (step73 < 0 && _y >= limit73); _y = ((int)(0 + _y + step73))) {
 //BA.debugLineNum = 118;BA.debugLine="found = found AND buttons(x, y).Text = Player";
_found = _found && (mostCurrent._buttons[_x][_y].getText()).equals(_player);
 }
};
 //BA.debugLineNum = 120;BA.debugLine="If found = True Then Return True";
if (_found==anywheresoftware.b4a.keywords.Common.True) { 
if (true) return anywheresoftware.b4a.keywords.Common.True;};
 }
};
 //BA.debugLineNum = 123;BA.debugLine="For y = 0 To 2";
{
final int step78 = 1;
final int limit78 = (int) (2);
for (_y = (int) (0); (step78 > 0 && _y <= limit78) || (step78 < 0 && _y >= limit78); _y = ((int)(0 + _y + step78))) {
 //BA.debugLineNum = 124;BA.debugLine="found = True";
_found = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 125;BA.debugLine="For x = 0 To 2";
{
final int step80 = 1;
final int limit80 = (int) (2);
for (_x = (int) (0); (step80 > 0 && _x <= limit80) || (step80 < 0 && _x >= limit80); _x = ((int)(0 + _x + step80))) {
 //BA.debugLineNum = 126;BA.debugLine="found = found AND buttons(x, y).Text = Player";
_found = _found && (mostCurrent._buttons[_x][_y].getText()).equals(_player);
 }
};
 //BA.debugLineNum = 128;BA.debugLine="If found = True Then Return True";
if (_found==anywheresoftware.b4a.keywords.Common.True) { 
if (true) return anywheresoftware.b4a.keywords.Common.True;};
 }
};
 //BA.debugLineNum = 131;BA.debugLine="found = True";
_found = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 132;BA.debugLine="For i = 0 To 2";
{
final int step86 = 1;
final int limit86 = (int) (2);
for (_i = (int) (0); (step86 > 0 && _i <= limit86) || (step86 < 0 && _i >= limit86); _i = ((int)(0 + _i + step86))) {
 //BA.debugLineNum = 133;BA.debugLine="found = found AND buttons(i, i).Text = Player";
_found = _found && (mostCurrent._buttons[_i][_i].getText()).equals(_player);
 }
};
 //BA.debugLineNum = 135;BA.debugLine="If found = True Then Return True";
if (_found==anywheresoftware.b4a.keywords.Common.True) { 
if (true) return anywheresoftware.b4a.keywords.Common.True;};
 //BA.debugLineNum = 137;BA.debugLine="found = True";
_found = anywheresoftware.b4a.keywords.Common.True;
 //BA.debugLineNum = 138;BA.debugLine="For i = 0 To 2";
{
final int step91 = 1;
final int limit91 = (int) (2);
for (_i = (int) (0); (step91 > 0 && _i <= limit91) || (step91 < 0 && _i >= limit91); _i = ((int)(0 + _i + step91))) {
 //BA.debugLineNum = 139;BA.debugLine="found = found AND buttons(i, 2 - i).Text = Player";
_found = _found && (mostCurrent._buttons[_i][(int) (2-_i)].getText()).equals(_player);
 }
};
 //BA.debugLineNum = 141;BA.debugLine="If found = True Then Return True";
if (_found==anywheresoftware.b4a.keywords.Common.True) { 
if (true) return anywheresoftware.b4a.keywords.Common.True;};
 //BA.debugLineNum = 142;BA.debugLine="Return False";
if (true) return anywheresoftware.b4a.keywords.Common.False;
 //BA.debugLineNum = 143;BA.debugLine="End Sub";
return false;
}

public static void initializeProcessGlobals() {
    
    if (main.processGlobalsRun == false) {
	    main.processGlobalsRun = true;
		try {
		        main._process_globals();
		
        } catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
}public static String  _globals() throws Exception{
 //BA.debugLineNum = 22;BA.debugLine="Sub Globals";
 //BA.debugLineNum = 25;BA.debugLine="Dim ButtonsText(3,3) As String";
mostCurrent._buttonstext = new String[(int) (3)][];
{
int d0 = mostCurrent._buttonstext.length;
int d1 = (int) (3);
for (int i0 = 0;i0 < d0;i0++) {
mostCurrent._buttonstext[i0] = new String[d1];
java.util.Arrays.fill(mostCurrent._buttonstext[i0],"");
}
}
;
 //BA.debugLineNum = 26;BA.debugLine="Private main_pnl As Panel";
mostCurrent._main_pnl = new anywheresoftware.b4a.objects.PanelWrapper();
 //BA.debugLineNum = 27;BA.debugLine="Private lbl As Label";
mostCurrent._lbl = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 28;BA.debugLine="Private player_lbl As Label";
mostCurrent._player_lbl = new anywheresoftware.b4a.objects.LabelWrapper();
 //BA.debugLineNum = 29;BA.debugLine="Dim buttons(3,3) As Button";
mostCurrent._buttons = new anywheresoftware.b4a.objects.ButtonWrapper[(int) (3)][];
{
int d0 = mostCurrent._buttons.length;
int d1 = (int) (3);
for (int i0 = 0;i0 < d0;i0++) {
mostCurrent._buttons[i0] = new anywheresoftware.b4a.objects.ButtonWrapper[d1];
for (int i1 = 0;i1 < d1;i1++) {
mostCurrent._buttons[i0][i1] = new anywheresoftware.b4a.objects.ButtonWrapper();
}
}
}
;
 //BA.debugLineNum = 30;BA.debugLine="Dim j,i As Int =0";
_j = 0;
_i = (int) (0);
 //BA.debugLineNum = 31;BA.debugLine="End Sub";
return "";
}
public static String  _newgame() throws Exception{
int _x = 0;
int _y = 0;
 //BA.debugLineNum = 102;BA.debugLine="Sub NewGame";
 //BA.debugLineNum = 103;BA.debugLine="For x = 0 To 2";
{
final int step62 = 1;
final int limit62 = (int) (2);
for (_x = (int) (0); (step62 > 0 && _x <= limit62) || (step62 < 0 && _x >= limit62); _x = ((int)(0 + _x + step62))) {
 //BA.debugLineNum = 104;BA.debugLine="For y = 0 To 2";
{
final int step63 = 1;
final int limit63 = (int) (2);
for (_y = (int) (0); (step63 > 0 && _y <= limit63) || (step63 < 0 && _y >= limit63); _y = ((int)(0 + _y + step63))) {
 //BA.debugLineNum = 105;BA.debugLine="buttons(x, y).Text = \"\"";
mostCurrent._buttons[_x][_y].setText((Object)(""));
 }
};
 }
};
 //BA.debugLineNum = 108;BA.debugLine="player_lbl.Text = \"X\"";
mostCurrent._player_lbl.setText((Object)("X"));
 //BA.debugLineNum = 109;BA.debugLine="End Sub";
return "";
}
public static String  _process_globals() throws Exception{
 //BA.debugLineNum = 16;BA.debugLine="Sub Process_Globals";
 //BA.debugLineNum = 20;BA.debugLine="End Sub";
return "";
}
public static String  _restart_click() throws Exception{
 //BA.debugLineNum = 65;BA.debugLine="Sub restart_Click";
 //BA.debugLineNum = 66;BA.debugLine="NewGame";
_newgame();
 //BA.debugLineNum = 67;BA.debugLine="End Sub";
return "";
}
}
