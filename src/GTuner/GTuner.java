package GTuner;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.RecordControl;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.media.MediaException;

public class GTuner extends MIDlet implements CommandListener {
    public GTuner() { }
    private Display display;
    MyCanvas canvas = new MyCanvas();
    int infos=10;
    String info[]=new String[infos];
    int w=0, h=0;
    Font f1 = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    Font f2 = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    Player p=null;
    byte[] recordedAudioArray = null;
    RecordControl rc;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    short[] sound=null;
    int sound_size = 0;
    long timec=0;
    boolean light = true;
    boolean recording = false;
    boolean tuner_on = true;
    boolean clipping = false;
    int min_sample_time = 50;
    long start_time=0, stop_time=0;
    int sample_rate=0;
    double mpi2 = -2.0 * Math.PI;
    double sqrt_624 = Math.sqrt(6)/24;
    double sqrt_23 = Math.sqrt(((double)2)/3);
    double mag[];
    int imaxmag=0;
    double freqi=0;
    double root2 = 1.0594630943593;
    double rootroot2 = 1.029302236643492;
    int struna=0;
    int timer_period=50;
    class MyTimerTask extends TimerTask {
        public void run(){
            if(timec%60==0&&light){
                display.flashBacklight(0);
                timec=0;
            }
            timec++;
            if(recording){
                stop_time = System.currentTimeMillis();
                if(stop_time-start_time>=min_sample_time){
                    record_stop();
                    canvas.repaint();
                }
            }
        }
    }
    Timer timer1 = new Timer();
    MyTimerTask timer1_task = new MyTimerTask();

    protected void startApp() {
        display = Display.getDisplay(this);
        canvas.setFullScreenMode(true);
        w=canvas.getWidth();
        h=canvas.getHeight();
        display.setCurrent(canvas);
        timer1.schedule(timer1_task, 0, timer_period);
        for(int i=0; i<infos; i++) info[i]="";
        try {
            p = Manager.createPlayer("capture://audio?encoding=pcm");
            p.realize();
            rc = (RecordControl) p.getControl("RecordControl");
            p.start();
        } catch (IOException ex) {
            error(ex);
        } catch (MediaException ex) {
            error(ex);
        }
        //info_norp("Start aplikacji");
        canvas.repaint();
        record_start();
    }
    public class MyCanvas extends Canvas {
        public void paint(Graphics g) {
            g.setColor(0x000000);
            g.fillRect(0,0,w,h);
            g.setColor(0x252525);
            g.setFont(f2);
            g.drawString("Igrek",w,0,Graphics.TOP|Graphics.RIGHT);
            if(sound_size>0&&imaxmag>0){
                //wykres fft
                g.setColor(0x006000);
                g.drawLine(0, h/2, w, h/2); //oś
                g.setColor(0xf5ee92);
                g.drawLine(imaxmag*w*2/sound_size, h/2-3, imaxmag*w*2/sound_size, h/2+3); //maxmag
                g.setColor(0x00c000);
                for(int i=1; i<sound_size/2-1; i++){
                    g.drawLine(i*w*2/sound_size, h/2-(int)(mag[i]*256/1500000), (i+1)*w*2/sound_size, h/2-(int)(mag[i+1]*256/1500000));
                }
                //pasek amplitudy
                if(clipping){
                    g.setColor(0x600000);
                }else{
                    g.setColor(0x606060);
                }
                g.fillRect(0,175,(int)(mag[imaxmag]/1500000*w),15);
                //odczytany dźwięk
                if(freqi>0){
                    g.setColor(0x00a0a0);
                    g.drawString("f: "+freqi+" Hz",0,160,Graphics.TOP|Graphics.LEFT);
                    g.drawString("Amplituda: "+floor3(mag[imaxmag]),0,175,Graphics.TOP|Graphics.LEFT);
                    g.drawString("Rozmiar próbki: "+sound_size,0,190,Graphics.TOP|Graphics.LEFT);
                    int s = get_sound(freqi);
                    double wzor = get_sound_f(s);
                    double pos=0;
                    if(freqi>wzor){
                        pos = (freqi-wzor)/(wzor*rootroot2-wzor);
                    }else{
                        pos = -(wzor-freqi)/(wzor-wzor/rootroot2);
                    }
                    if(pos<-1) pos=-1;
                    if(pos>1) pos=1;
                    g.drawString((freqi>wzor?"+":"")+floor3((freqi-wzor)/wzor*100)+" %",w/2,205,Graphics.TOP|Graphics.HCENTER);
                    g.drawString(""+get_sound_name(s)+" - "+floor3(wzor)+" Hz",w/2,220,Graphics.TOP|Graphics.HCENTER);
                    g.setColor(0x006000);
                    g.drawLine(w/2, 235, w/2, 265); //wzor
                    g.drawLine(0, 235, w, 235);
                    g.drawLine(0, 265, w, 265);
                    if(mag[imaxmag]<150000*freqi/440){ //próg amplitudy
                        g.setColor(0x9a9a9a);
                    }else if((pos>=0?pos:-pos)<0.11){
                        g.setColor(0x00e000);
                    }else{
                        g.setColor(0xe00000);
                    }
                    g.fillRect((int)((pos+1)*w/2)-2,235,4,30);
                    if(clipping){
                        g.setColor(0xf00000);
                        g.drawString("Trzaski",w/2,265,Graphics.BOTTOM|Graphics.HCENTER);
                    }
                }
            }
            if(info[0].length()>0){
                g.setFont(f1);
                g.setColor(0xa0a0a0);
                for(int i=0; i<infos; i++){
                    g.drawString(info[i],w,i*15,Graphics.TOP|Graphics.RIGHT);
                }
                g.setFont(f2);
            }
            
            g.setFont(f1);
            g.setColor(0x808080);
            g.drawString("Timer_period: "+timer_period,0,280,Graphics.TOP|Graphics.LEFT);
            g.setFont(f2);
            
            g.setColor(0xffffff);
            g.drawString(tuner_on?"Wyłącz":"Włącz",0,h,Graphics.BOTTOM|Graphics.LEFT);
            g.drawString("Wyjdź",w,h,Graphics.BOTTOM|Graphics.RIGHT);
            if(struna>0){
                String strunastr="Struna ";
                if(struna==1) strunastr+="E1";
                if(struna==2) strunastr+="H2";
                if(struna==3) strunastr+="G3";
                if(struna==4) strunastr+="D4";
                if(struna==5) strunastr+="A5";
                if(struna==6) strunastr+="E6";
                if(struna==7) strunastr="Dźwięk A4";
                if(struna==8) strunastr="AutoPeak";
                g.drawString(strunastr,w/2,h,Graphics.BOTTOM|Graphics.HCENTER);
            }
        }
        public void keyPressed(int keyCode){
            if(keyCode==-6){ //left button
                tuner_on = !tuner_on;
                if(tuner_on){
                    record_start();
                }else{
                    record_stop();
                }
            }
            if(keyCode==-5){ //middle button
                if(struna>0){
                    struna=0;
                }else{
                    struna=6;
                }
            }
            if(keyCode==-7){ //right button
                exit();
            }
            if(keyCode==Canvas.KEY_NUM0) struna=0;
            if(keyCode==Canvas.KEY_NUM1) struna=1;
            if(keyCode==Canvas.KEY_NUM2) struna=2;
            if(keyCode==Canvas.KEY_NUM3) struna=3;
            if(keyCode==Canvas.KEY_NUM4) struna=4;
            if(keyCode==Canvas.KEY_NUM5) struna=5;
            if(keyCode==Canvas.KEY_NUM6) struna=6;
            if(keyCode==Canvas.KEY_NUM7) struna=7;
            if(keyCode==Canvas.KEY_NUM8) struna=8;
            if(keyCode==-4){ //right
                if(struna<6) struna++;
            }
            if(keyCode==-3){ //left
                if(struna>1) struna--;
            }
            if(keyCode==-1){ //up
                timer_period-=10;
                change_timer(timer_period);
            }
            if(keyCode==-2){ //down
                timer_period+=10;
                change_timer(timer_period);
            }
            repaint();
        }
        public void keyRepeated(int keyCode){
            keyPressed(keyCode);
        }
    }
    void change_timer(int period){
        timer1.cancel();
        //timer1_task.cancel();
        timer1 = new Timer();
        timer1_task = new MyTimerTask();
        timer1.schedule(timer1_task, 0, period);
    }
    void info(String in){
        for(int i=infos-1; i>0; i--){
            info[i]=info[i-1];
        }
        info[0]=in;
        canvas.repaint();
    }
    void info_norp(String in){
        for(int i=infos-1; i>0; i--){
            info[i]=info[i-1];
        }
        info[0]=in;
    }
    void error(Exception e){
        info("Blad: "+e.toString());
        e.printStackTrace();
    }
    double floor3(double in){
        return Math.floor(in*1000)/1000;
    }
    void analyze(){
        if(sound_size<=0) return;
        long suma=0;
        for(int i=0; i<sound_size; i++){
            suma+=sound[i];
        }
        short level0 = (short)(((double)suma)/sound_size);
        info("Level0: "+level0);
        suma=0;
        for(int i=0; i<sound_size; i++){
            if(sound[i]>level0){
                suma+=sound[i]-level0;
            }else{
                suma+=level0-sound[i];
            }
        }
        double vol = ((double)suma)/sound_size;
        info("Volume: "+vol);
    }
    boolean search_for_clipping(){
        for(int i=0; i<sound_size; i++){
            if(sound[i]==32767||sound[i]==-32768) return true;
        }
        return false;
    }
    void cut_to_radix2(){
        if(sound_size<=0) return;
        int pom1 = 1;
        while(pom1<=sound_size){
            pom1*=2;
        }
        short[] nowa = new short[pom1/2];
        for(int i=0; i<pom1/2; i++){
            nowa[i] = sound[i+sound_size-pom1/2];
        }
        sound_size = pom1/2;
        sound = nowa;
    }
    void record_start(){
        if(!recording){
            try{
                //p = Manager.createPlayer("capture://audio?encoding=pcm");
                //p.realize();
                //rc = (RecordControl) p.getControl("RecordControl");
                output.reset();
                rc.setRecordStream(output);
                //rc.setRecordSizeLimit(min_sample_size+44);
                //p.start();
                rc.startRecord();
                start_time = System.currentTimeMillis();
                recording=true;
            } catch (Exception ex) {
                error(ex);
            }
        }
    }
    
    void record_stop(){
        if(recording){
            try{
                recording=false;
                rc.commit();
                //p.close();
                recordedAudioArray = output.toByteArray();
                int rozmiar = recordedAudioArray[40];
                rozmiar += recordedAudioArray[41]*256;
                rozmiar += recordedAudioArray[42]*256*256;
                rozmiar += recordedAudioArray[43]*256*256*256;
                sample_rate = recordedAudioArray[24];
                sample_rate += recordedAudioArray[25]*256;
                sample_rate += recordedAudioArray[26]*256*256;
                sample_rate += recordedAudioArray[27]*256*256*256;
                if(rozmiar+44!=output.size()){
                    info("rozmiar: "+rozmiar+", output.size: "+output.size());
                    info("Błąd: konflikt rozmiarów");
                    sound_size=0;
                    return;
                }
                sound_size = rozmiar/2;
                if(rozmiar%2==1){
                    info("Błąd: nieparzysta liczba bajtow");
                    sound_size=0;
                    return;
                }
                if(sound_size<=0){
                    info("pusta tablica");
                    sound_size=0;
                }else{
                    sound = new short[sound_size];
                    for(int i=0; i<sound_size*2; i+=2) {
                        sound[i/2] = (short) ((recordedAudioArray[i+45] << 8) + (recordedAudioArray[i+44]&0xFF));
                    }
                    cut_to_radix2();
                    //clipping = search_for_clipping();
                    zespolona[] x = new zespolona [sound_size];
                    for(int i=0; i<sound_size; i++){
                        x[i] = new zespolona();
                        x[i].re = (double) sound[i];
                        x[i].im = 0;
                    }
                    zespolona[] X = FFT_simple(x,sound_size);
                    mag = new double [sound_size];
                    for(int i=0; i<sound_size; i++){
                        mag[i] = zesp_magnitude(X[i]);
                    }
                    if(struna==0){
                        imaxmag=1;
                        for(int i=2; i<sound_size/2; i++){
                            if(mag[i] > mag[imaxmag]) imaxmag=i;
                        }
                    }else if(struna==8){ //autopeak
                        double maxmag=0;
                        for(int i=2; i<sound_size/2; i++){
                            if(mag[i] > maxmag) maxmag = mag[i];
                        }
                        int left;
                        maxmag *= 0.5;
                        for(left=2; left<sound_size/2; left++){
                            if(mag[left] >= maxmag){
                                break;
                            }
                        }
                        int right;
                        for(right=left+1; right<sound_size/2; right++){
                            if(mag[right] < maxmag){
                                break;
                            }
                        }
                        imaxmag=left;
                        for(int i=left; i<right; i++){
                            if(mag[i] > mag[imaxmag]) imaxmag=i;
                        }
                    }else{
                        double wzor = get_sound_f(get_sound_by_struna(struna));
                        int left = (int)Math.floor(wzor/rootroot2*sound_size/sample_rate);
                        int right = (int)Math.ceil(wzor*rootroot2*sound_size/sample_rate);
                        imaxmag=left;
                        for(int i=left+1; i<right; i++){
                            if(mag[i] > mag[imaxmag]) imaxmag=i;
                        }
                    }
                    if(imaxmag>0&&imaxmag<sound_size-1){
                        freqi=quinn2(X,imaxmag)/sound_size*sample_rate;
                    }else{
                        freqi=0;
                    }
                }
                Thread.currentThread().sleep(1);
            } catch (Exception e) {
                error(e);
            }
            if(tuner_on) record_start();
        }
    }
    
    class zespolona{
        double re;
        double im;
    };
    zespolona biegun_do_zesp(double r, double fi){
        zespolona z = new zespolona();
        z.re = r*Math.cos(fi);
        z.im = r*Math.sin(fi);
        return z;
    }
    double zesp_magnitude(zespolona z){
        return Math.sqrt(z.re*z.re + z.im*z.im);
    }
    zespolona zesp_add(zespolona left, zespolona right){
        zespolona result = new zespolona();
        result.re = left.re + right.re;
        result.im = left.im + right.im;
        return result;
    }
    zespolona zesp_sub(zespolona left, zespolona right){
        zespolona result = new zespolona();
        result.re = left.re - right.re;
        result.im = left.im - right.im;
        return result;
    }
    zespolona zesp_mult(zespolona left, zespolona right){
        zespolona result = new zespolona();
        result.re = left.re*right.re - left.im*right.im;
        result.im = left.re*right.im + left.im*right.re;
        return result;
    }
    
    zespolona[] FFT_simple(zespolona[] x, int N){
        zespolona[] X = new zespolona [N];
        if(N==1){
            X[0] = x[0];
            return X;
        }
        zespolona[] d = new zespolona [N/2];
        zespolona[] e = new zespolona [N/2];
        for(int k=0; k<N/2; k++) {
            e[k] = x[2*k];
            d[k] = x[2*k+1];
        }
        zespolona[] D = FFT_simple(d, N/2);
        zespolona[] E = FFT_simple(e, N/2);
        for(int k=0; k<N/2; k++) {
            double pom1_re = Math.cos(mpi2*k/N);
            double pom1_im = Math.sin(mpi2*k/N);
            double old_re = D[k].re;
            D[k].re = pom1_re*old_re - pom1_im*D[k].im;
            D[k].im = pom1_re*D[k].im + pom1_im*old_re;
        }
        for(int k=0; k<N/2; k++) {
            X[k] = zesp_add(E[k],D[k]);
            X[k+N/2] = zesp_sub(E[k],D[k]);
        }
        return X;
    }
    
    int get_sound(double f){
        if(f<=0) return 0;
        int s = 48+9; //A4
        double a4 = 440;
        while(f<a4){
            s--;
            a4/=root2;
        }
        a4=440*root2;
        while(f>=a4){
            s++;
            a4*=root2;
        }
        if(f>get_sound_f(s)*Math.sqrt(root2)){
            s++;
        }
        return s;
    }
    double get_sound_f(int s){
        double wzor = 440;
        for(int i=0; i<s-57; i++){
            wzor*=root2;
        }
        for(int i=0; i<57-s; i++){
            wzor/=root2;
        }
        return wzor;
    }
    String get_sound_name(int s){
        int octave = s/12;
        int s2=s%12;
        String soundn="";
        if(s2==0) soundn="C";
        if(s2==1) soundn="C#";
        if(s2==2) soundn="D";
        if(s2==3) soundn="D#";
        if(s2==4) soundn="E";
        if(s2==5) soundn="F";
        if(s2==6) soundn="F#";
        if(s2==7) soundn="G";
        if(s2==8) soundn="G#";
        if(s2==9) soundn="A";
        if(s2==10) soundn="B";
        if(s2==11) soundn="H";
        soundn+=" "+octave;
        if(s==52) soundn+=" (struna E1)";
        if(s==47) soundn+=" (struna H2)";
        if(s==43) soundn+=" (struna G3)";
        if(s==38) soundn+=" (struna D4)";
        if(s==33) soundn+=" (struna A5)";
        if(s==28) soundn+=" (struna E6)";
        return soundn;
    }
    int get_sound_by_struna(int str){
        if(str==1) return 52;
        if(str==2) return 47;
        if(str==3) return 43;
        if(str==4) return 38;
        if(str==5) return 33;
        if(str==6) return 28;
        if(str==7) return 57;
        return 0;
    }
    
    double pow(double base, int exp){
        if(exp==0) return 1;
        double res = base;
        for(int i=exp;i>1;i--){
            res *= base;
        }
        return res;
    }
    double log(double x) {
        long l = Double.doubleToLongBits(x);
        long exp = ((0x7ff0000000000000L & l) >> 52) - 1023;
        double man = (0x000fffffffffffffL & l) / (double)0x10000000000000L + 1.0;
        double lnm = 0.0;
        double a = (man - 1) / (man + 1);
        for(int n=1; n<7; n+=2){
            lnm += pow(a,n) / n;
        }
        return 2 * lnm + exp*0.69314718055994530941723212145818;
    }
    double quinn2_tau(double x){
        return 0.25*log(3*x*x+6*x+1) - sqrt_624*log((x+1-sqrt_23)/(x+1+sqrt_23));
    }
    double quinn2(zespolona[] X, int imax){
        double ap = (X[imax+1].re*X[imax].re+X[imax+1].im*X[imax].im) / (X[imax].re*X[imax].re+X[imax].im*X[imax].im);
        double dp = -ap/(1-ap);
        double am = (X[imax-1].re*X[imax].re+X[imax-1].im*X[imax].im) / (X[imax].re*X[imax].re+X[imax].im*X[imax].im);
        double dm = am/(1-am);
        double d = (dp+dm)/2 + quinn2_tau(dp*dp) - quinn2_tau(dm*dm);
        if((d>=0?d:-d)>1) return 0;
        return imax + d;
    }
    
    void exit(){
        destroyApp(false);
    }
    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { notifyDestroyed(); }
    public void commandAction(Command c, Displayable d) { }
}