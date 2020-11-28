function modSig = fskmod(data)
    f=3000;
    fs=30000;
    Ts=1/fs;
    T=1/f;
    M=1000;
    n=M*length(data);
    t=0:Ts:n*T;
    car=sin(2*pi*t*f);
    figure;
    %subplot(3,1,1);
    %plot(car,'r');
    %xlabel('Samples(Carrier signal)');
    %ylabel('Amplitude');
    % Converting data bits into pulse
    tp=0:Ts:M*T;
    exData=[];
     for i=1:length(data)
         for j=1:length(tp)-1
             exData=[exData data(i)];
         end
     end
     exData(1,size(exData)+1)=exData(1,size(exData));

     %subplot(3,1,2)
     %plot(exData,'g','Linewidth',2)
     %xlabel('Samples(Message signal)');
     %ylabel('Amplitude');

     % FSK Modulation schemes
     deltaf=.5;
     fh=f + (f*deltaf);
     fl=f - (f*deltaf);

     t=0:Ts:(T*M);

     carh=sin(2*pi*t*fh);       %High frequency carrier for data bit 1
     carl=sin(2*pi*t*fl);       %Low frequency carrier for data bit 0

     modSig=[];

     for i=1:length(data)
         if(data(i)==1)
             modSig=[modSig carh];
         else
             modSig=[modSig carl];
         end
     end

     %subplot(3,1,3);
     %plot(modSig,'b');
     %xlabel('Samples(FSK Modulated signal)');
     %ylabel('Amplitude');
end