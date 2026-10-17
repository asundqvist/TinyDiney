import React from 'react';

export default function FoodArt({ type = 'pancakes', className = '' }) {
  return (
    <svg className={className} viewBox="0 0 400 330" aria-hidden="true">
      <ellipse cx="204" cy="293" rx="148" ry="15" fill="#342a1c" opacity=".09" />
      <ellipse cx="200" cy="175" rx="163" ry="140" fill="#e9e2d1" />
      <ellipse cx="200" cy="167" rx="163" ry="140" fill="#fffdf6" />
      <ellipse cx="200" cy="167" rx="135" ry="114" fill="none" stroke="#e9e3d4" strokeWidth="2" />
      {type === 'pancakes' ? <>
        {[208, 189, 170, 151].map((y, i) => <g key={y} transform={`rotate(-8 200 ${y})`}>
          <ellipse cx="194" cy={y + 7} rx="96" ry="42" fill="#b96926" />
          <ellipse cx="194" cy={y} rx="96" ry="42" fill={i % 2 ? '#ebae51' : '#e9a346'} stroke="#cb8431" strokeWidth="2" />
          <ellipse cx="194" cy={y - 2} rx="81" ry="30" fill="none" stroke="#f6ca74" strokeWidth="3" opacity=".7" />
        </g>)}
        <path d="M166 131q27-24 62-2l-3 31q-5 14-12 1l-4-18q-10-8-14 4l-3 39q-8 12-13 0l-1-37z" fill="#b86028" />
        <path d="m176 119 31-5 15 17-31 7-16-9z" fill="#fff0a8" stroke="#eac875" strokeWidth="2" />
        {[[263, 209], [283, 190], [274, 228], [300, 214]].map(([x, y]) => <g key={x}><path d={`M${x-12} ${y-9}q15-12 25 0q-3 25-14 26q-13-5-11-26`} fill="#be402f" /><path d={`m${x-10} ${y-9} 11 5 10-8-9 1-3-8-2 9z`} fill="#52734d" /><path d={`m${x-5} ${y+2} 2 3m6-5 2 3m-4 6 2 3`} stroke="#ffd48a" strokeWidth="2" /></g>)}
        {[[105, 170], [116, 187], [100, 198], [121, 207], [142, 220]].map(([x,y]) => <circle key={x} cx={x} cy={y} r="8" fill="#40546b" stroke="#293c53" strokeWidth="2" />)}
        <path d="m233 106 19-14-8 20 20-4-19 14-12-3z" fill="#4c754b" />
      </> : type === 'meatballs' ? <>
        {Array.from({length: 20}, (_, i) => <path key={i} d={`M${102+(i%5)*18} ${117+Math.floor(i/5)*23}q-17 0-13 15t18 3`} fill="none" stroke={i%2 ? '#e7bd62' : '#f1cc7a'} strokeWidth="9" strokeLinecap="round" transform="rotate(-12 150 170)" />)}
        <path d="M212 133q75-13 88 50t-77 59q-42-5-32-52z" fill="#bb8a58" opacity=".65" />
        {[[230,143],[269,156],[216,180],[260,193],[233,218]].map(([x,y]) => <g key={x}><circle cx={x} cy={y} r="20" fill="#805036" /><circle cx={x-4} cy={y-5} r="14" fill="#98613e" /><path d={`m${x-8} ${y-3} 3-3m7 12 3-2m-2-15 2 2`} stroke="#65402e" strokeWidth="3" strokeLinecap="round" /></g>)}
        {[[135,235],[146,250],[162,238],[157,257],[177,247]].map(([x,y]) => <circle key={x} cx={x} cy={y} r="8" fill="#ad3633" />)}
        <path d="m264 116 23-17-6 26 20 3-28 7z" fill="#5a7a40" />
      </> : type === 'pizza' ? <>
        <circle cx="200" cy="172" r="96" fill="#dba45b" />
        <circle cx="200" cy="172" r="83" fill="#bc5038" />
        <circle cx="200" cy="172" r="73" fill="#f2d594" />
        {[[170,132],[226,139],[151,188],[205,211],[248,190]].map(([x,y]) => <circle key={x} cx={x} cy={y} r="16" fill="#fff0c4" />)}
        <path d="m184 169q-27-27-32-8t32 8m28 23q-1-33 18-26t-18 26m-5-67q-24-17-27 0t27 0" fill="#547249" />
        <path d="m200 172 68 64m-68-64-70 60m70-60 5-95" stroke="#dba45b" strokeWidth="3" />
      </> : type === 'black-pudding' ? <>
        {[[142,125],[170,162],[196,199]].map(([x,y]) => <g key={x} transform={`rotate(-20 ${x} ${y})`}>
          <ellipse cx={x} cy={y+9} rx="50" ry="31" fill="#342528" />
          <ellipse cx={x} cy={y} rx="50" ry="31" fill="#654538" stroke="#8c6045" strokeWidth="3" />
          <ellipse cx={x} cy={y-2} rx="43" ry="24" fill="#423034" />
          <path d={`m${x-24} ${y-7} 4 2m15 13 4-2m-1-22 3 2m18 10 3-3m-43 20 4 1`} stroke="#95674c" strokeWidth="3" strokeLinecap="round" />
        </g>)}
        <path d="M242 126q34-15 49 13t-16 45q-41 6-43-25z" fill="#a52f3d" />
        {[[248,145],[264,135],[281,149],[249,166],[268,166],[278,179]].map(([x,y]) => <g key={x+'-'+y}><circle cx={x} cy={y} r="7" fill="#ba3c4a" /><circle cx={x-2} cy={y-2} r="2" fill="#df7e79" /></g>)}
        {Array.from({length:12},(_,i) => <path key={i} d={`M${113+(i%4)*14} ${228+Math.floor(i/4)*10}q20-16 38-5`} fill="none" stroke={i%2 ? '#e5d599' : '#f3e6b7'} strokeWidth="4" strokeLinecap="round" transform="rotate(-10 150 245)" />)}
      </> : type === 'burger' ? <>
        {Array.from({length:8},(_,i) => <rect key={i} x={244+(i%4)*12} y={181+Math.floor(i/4)*18} width="10" height="65" rx="3" fill={i%2 ? '#edc16b' : '#f4d18a'} transform="rotate(17 270 215)" />)}
        <path d="M99 203h140v12q-5 24-70 24t-70-24z" fill="#cc8e43" />
        <ellipse cx="169" cy="204" rx="70" ry="15" fill="#efbd70" />
        <rect x="101" y="181" width="136" height="25" rx="12" fill="#624333" />
        <path d="m102 176 134 2-21 18-34-7-29 12-24-17-26 5z" fill="#efcd69" />
        <path d="m97 161 17-8 18 6 18-9 19 8 18-7 20 8 17-6 18 10-7 13-20-3-19 8-23-5-19 6-21-8-21 1z" fill="#71944d" />
        <ellipse cx="169" cy="153" rx="65" ry="13" fill="#c55b43" />
        <path d="M99 145q3-62 70-62t70 62q-67 22-140 0z" fill="#e0a452" stroke="#c78a40" strokeWidth="2" />
        <path d="M110 132q14-39 59-39t58 39" fill="none" stroke="#f0c77e" strokeWidth="5" strokeLinecap="round" />
        {[[139,111],[164,101],[190,110],[211,128],[174,127],[126,133]].map(([x,y]) => <path key={x} d={`m${x} ${y} 5-3`} stroke="#fff0c0" strokeWidth="3" strokeLinecap="round" />)}
      </> : type === 'chicken' || type === 'fish' ? <>
        {Array.from({length: 9}, (_,i) => <rect key={i} x={108+(i%3)*20} y={118+Math.floor(i/3)*30} width="13" height="67" rx="5" fill={i%2 ? '#e9bb61' : '#f0ce84'} transform={`rotate(-20 ${140} ${175})`} />)}
        {type === 'chicken' ? [[206,120],[245,151],[206,184],[246,215]].map(([x,y]) => <g key={y} transform={`rotate(15 ${x} ${y})`}><rect x={x-19} y={y-13} width="43" height="32" rx="13" fill="#c98a3e" /><rect x={x-17} y={y-12} width="39" height="26" rx="12" fill="#e1ac59" /><path d={`m${x-8} ${y-4} 5 2m10 7 4-3m-6-10 3 1`} stroke="#ba7c32" strokeWidth="2" /></g>) : <>
          <path d="M223 102q29-5 44 29l23 60q8 30-15 38t-40-20l-30-67q-13-31 18-40z" fill="#c98a3e" />
          <path d="M225 108q23-3 36 27l21 59q6 24-10 28t-32-19l-28-65q-10-26 13-30z" fill="#e4b462" />
          <path d="m225 128 6 9m13 10 8 12m-17 1 5 10m18 9 7 14m-14 5 5 8" stroke="#be853b" strokeWidth="3" strokeLinecap="round" />
        </>}
        {type === 'fish' && [[120,224],[138,217],[156,225],[175,219],[129,241],[148,239],[169,239],[187,237],[144,256],[165,256]].map(([x,y]) => <g key={x+'-'+y}><circle cx={x} cy={y} r="7" fill="#638b46" /><circle cx={x-2} cy={y-2} r="4" fill="#94b96a" /></g>)}
        <ellipse cx="271" cy="243" rx="28" ry="22" fill="#e4ded0" /><ellipse cx="271" cy="239" rx="23" ry="17" fill="#fff0cd" />
      </> : <>
        <path d="M108 117q91-32 178 5l-10 106q-92 33-158-8z" fill="#e1b86b" />
        {Array.from({length: 30},(_,i) => <path key={i} d={`M${119+(i%6)*27} ${127+Math.floor(i/6)*19}q-14 4-6 14t15-2`} fill="none" stroke={i%3 ? '#f6d883' : '#ebc46a'} strokeWidth="10" strokeLinecap="round" />)}
        <path d="M155 136q57-28 85 13t-14 65q-63 12-79-21t8-57" fill={type === 'peas' ? '#a4bc71' : '#c64e32'} />
        {type === 'peas' && <>
          {[[166,143],[191,136],[219,148],[153,169],[180,161],[206,172],[231,179],[167,191],[193,199],[216,211],[144,211],[251,151],[256,216],[179,229]].map(([x,y]) => <g key={x + '-' + y}><circle cx={x} cy={y} r="8" fill="#638b46" /><circle cx={x-2} cy={y-2} r="5.5" fill="#86ad58" /><circle cx={x-3} cy={y-3} r="2" fill="#b4d17d" /></g>)}
          <path d="M245 104q39-26 62 2-24 29-62-2Z" fill="#547b3f" />
          <path d="M250 104q29-13 50 2" fill="none" stroke="#a3c579" strokeWidth="3" />
          {[260,275,290].map(x => <circle key={x} cx={x} cy="105" r="6" fill="#92b969" />)}
        </>}
        <path d="m195 158 25-18-5 26 18 8-27 9-18-12z" fill="#50743e" />
        <path d="m139 139 12-8m85 68 13 3m-70-9 10 5m35-55 12-4" stroke="#ffebbd" strokeWidth="4" strokeLinecap="round" />
      </>}
    </svg>
  );
}
