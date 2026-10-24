package com.jaborzoda.evolvewarriors;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.graphics.*;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

public class MainActivity extends Activity {
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
    setContentView(new BattleView(this));
  }
  static class Unit {
    float x,y,hp,maxHp,speed,cooldown;
    boolean ally,elite;
    Unit(float x,float y,boolean ally,boolean elite,int level){
      this.x=x;this.y=y;this.ally=ally;this.elite=elite;
      maxHp=hp=(elite?130:38)+level*9; speed=(elite?14:24)+level*2;
    }
  }
  class BattleView extends View {
    Paint p=new Paint(3); Random rng=new Random();
    ArrayList<Unit> allies=new ArrayList<>(), foes=new ArrayList<>();
    SharedPreferences prefs; int wave,coins,kills,level;
    float spawn=0,blast=0; boolean paused=false,over=false,shop=false;
    long last=System.nanoTime(); float W=1000,H=600;
    BattleView(Context c){
      super(c); prefs=getSharedPreferences("save",0);
      wave=prefs.getInt("wave",1);coins=prefs.getInt("coins",0);
      kills=prefs.getInt("kills",0);level=prefs.getInt("level",0);
      for(int i=0;i<5+level;i++) allies.add(new Unit(100+rng.nextInt(80),230+rng.nextInt(150),true,false,level));
      spawnWave();
    }
    void save(){prefs.edit().putInt("wave",wave).putInt("coins",coins).putInt("kills",kills).putInt("level",level).apply();}
    void spawnWave(){
      int n=4+wave*2;
      for(int i=0;i<n;i++) foes.add(new Unit(820+rng.nextInt(100),210+rng.nextInt(190),false,wave%3==0&&i==n-1,wave/3));
    }
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);W=getWidth();H=getHeight();c.save();
      c.drawColor(Color.rgb(22,31,43));
      p.setShader(new LinearGradient(0,0,0,H,Color.rgb(31,52,69),Color.rgb(95,76,58),Shader.TileMode.CLAMP));
      c.drawRect(0,0,W,H,p);p.setShader(null);
      p.setColor(Color.rgb(53,61,55));c.drawRect(0,H*.63f,W,H,p);
      p.setColor(Color.rgb(76,77,63));for(int i=0;i<10;i++){float x=i*W/9f;c.drawRect(x,H*.62f,x+W*.03f,H,p);}
      p.setColor(Color.argb(80,10,10,10));for(int i=0;i<16;i++)c.drawCircle((i*97f+System.nanoTime()/1e8f*30)%W,H*.2f+(i%3)*20,4+i%6,p);
      float dt=Math.min(.05f,(System.nanoTime()-last)/1e9f);last=System.nanoTime();
      if(!paused&&!over&&!shop) update(dt);
      for(Unit u:allies)drawUnit(c,u);for(Unit u:foes)drawUnit(c,u);
      drawHud(c);c.restore();postInvalidateDelayed(16);
    }
    void update(float dt){
      spawn+=dt;
      if(spawn>2.8f&&foes.size()<4+wave*2){spawn=0;foes.add(new Unit(W*.85f+rng.nextInt(60),H*.35f+rng.nextInt(130),false,wave%3==0,wave/3));}
      moveTeam(allies,foes,dt);moveTeam(foes,allies,dt);
      for(int i=foes.size()-1;i>=0;i--)if(foes.get(i).hp<=0){Unit u=foes.remove(i);coins+=3+wave;kills++;if(kills%5==0){}save();}
      for(int i=allies.size()-1;i>=0;i--)if(allies.get(i).hp<=0)allies.remove(i);
      if(foes.isEmpty()){wave++;coins+=10;spawnWave();save();}
      if(allies.isEmpty()){over=true;}
      if(blast>0)blast-=dt;
    }
    void moveTeam(ArrayList<Unit> team,ArrayList<Unit> enemies,float dt){
      for(Unit u:team){
        Unit target=null;float best=999999;
        for(Unit e:enemies){float d=Math.abs(e.x-u.x)+Math.abs(e.y-u.y)*.6f;if(d<best){best=d;target=e;}}
        if(target==null)continue;
        float dx=target.x-u.x,dy=target.y-u.y,dist=(float)Math.sqrt(dx*dx+dy*dy);
        if(dist>(u.elite?55:34)){u.x+=dx/(dist+.01f)*u.speed*dt;u.y+=dy/(dist+.01f)*u.speed*dt;}
        else {u.cooldown-=dt;if(u.cooldown<=0){u.cooldown=.55f;target.hp-=(5+level*2)*(u.ally?1:.85f);}}
        u.x=Math.max(25,Math.min(W-30,u.x));u.y=Math.max(H*.42f,Math.min(H*.78f,u.y));
      }
    }
    void drawUnit(Canvas c,Unit u){
      float s=u.elite?1.6f:1f,x=u.x,y=u.y;
      int color=u.ally?Color.rgb(79,191,255):Color.rgb(234,83,72);
      p.setColor(Color.argb(90,0,0,0));c.drawOval(x-15*s,y+18*s,x+15*s,y+25*s,p);
      p.setColor(color);c.drawCircle(x,y-23*s,9*s,p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(4*s);
      c.drawLine(x,y-13*s,x,y+8*s,p);c.drawLine(x,y-6*s,x-9*s,y+1*s,p);c.drawLine(x,y-6*s,x+10*s,y-1*s,p);
      c.drawLine(x,y+8*s,x-8*s,y+19*s,p);c.drawLine(x,y+8*s,x+8*s,y+19*s,p);p.setStyle(Paint.Style.FILL);
      p.setColor(Color.DKGRAY);c.drawRect(x-14*s,y-40*s,x+14*s,y-36*s,p);
      p.setColor(Color.rgb(80,220,105));c.drawRect(x-14*s,y-40*s,x-14*s+28*s*Math.max(0,u.hp/u.maxHp),y-36*s,p);
      if(u.elite){p.setColor(Color.WHITE);p.setTextSize(12);c.drawText("ELITE",x-16,y-47,p);}
    }
    void text(Canvas c,String s,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));c.drawText(s,x,y,p);}
    void button(Canvas c,String s,float x,float y,float w,float h){
      p.setColor(Color.argb(220,18,25,34));c.drawRoundRect(x,y,x+w,y+h,12,12,p);
      p.setColor(Color.rgb(255,178,54));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRoundRect(x,y,x+w,y+h,12,12,p);p.setStyle(Paint.Style.FILL);
      text(c,s,x+12,y+h*.65f,18,Color.WHITE);
    }
    void drawHud(Canvas c){
      text(c,"EVOLVE  /  OR  DIE",22,32,26,Color.WHITE);
      text(c,"WAVE "+wave,22,62,19,Color.rgb(255,198,83));
      text(c,"✦ "+coins+"   EVOLUTION "+level,22,89,16,Color.WHITE);
      text(c,"SQUAD "+allies.size()+"   KILLS "+kills,22,113,16,Color.WHITE);
      button(c,"EVOLVE  ✦",W-185,18,165,42);button(c,"CLONE",W-185,68,165,42);
      button(c,blast>0?"BLAST!":"SUPER BLAST",W-185,118,165,42);
      if(paused||shop||over){
        p.setColor(Color.argb(190,5,9,15));c.drawRect(0,0,W,H,p);
        text(c,over?"SQUAD DEFEATED":shop?"EVOLUTION LAB":"PAUSED",W*.36f,H*.35f,36,Color.WHITE);
        if(over){button(c,"RETRY",W*.4f,H*.52f,160,52);button(c,"RESET SAVE",W*.4f,H*.62f,160,48);}
        else if(shop){button(c,"UPGRADE SQUAD — "+(50+level*35)+" COINS",W*.30f,H*.48f,W*.42f,52);button(c,"BACK TO BATTLE",W*.4f,H*.62f,180,48);}
        else button(c,"RESUME",W*.4f,H*.52f,180,52);
      }
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
      if(e.getAction()!=MotionEvent.ACTION_UP)return true;
      float x=e.getX(),y=e.getY();
      if(over){
        if(y>H*.50f&&y<H*.62f){over=false;allies.clear();foes.clear();for(int i=0;i<5+level;i++)allies.add(new Unit(100+rng.nextInt(80),230+rng.nextInt(150),true,false,level));spawnWave();}
        else if(y>=H*.62f){wave=1;coins=0;kills=0;level=0;save();over=false;allies.clear();foes.clear();for(int i=0;i<5;i++)allies.add(new Unit(100+rng.nextInt(80),230+rng.nextInt(150),true,false,0));spawnWave();}
        invalidate();return true;
      }
      if(shop){if(y>H*.45f&&y<H*.60f&&coins>=50+level*35){coins-=50+level*35;level++;for(Unit u:allies){u.maxHp+=10;u.hp+=10;}save();}else shop=false;invalidate();return true;}
      if(paused){paused=false;invalidate();return true;}
      if(x>W-200&&y<62){shop=true;invalidate();return true;}
      if(x>W-200&&y<112){if(coins>=20){coins-=20;for(int i=0;i<2;i++)allies.add(new Unit(100+rng.nextInt(60),250+rng.nextInt(120),true,false,level));save();}invalidate();return true;}
      if(x>W-200&&y<162){for(Unit u:foes)u.hp-=18+level*3;blast=1;invalidate();return true;}
      if(x<70&&y<45){paused=true;invalidate();return true;}
      for(Unit u:allies){u.x+=Math.signum(x-u.x)*25;u.y+=Math.signum(y-u.y)*12;}
      invalidate();return true;
    }
  }
}