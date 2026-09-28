'use strict';
const inherited=PicoTheme.vibrantColorScheme();
PicoTheme.install({scheme:'vibrant',colorScheme:{
 fillPrimary:inherited.fillPrimary,fillSecondary:inherited.fillSecondary,fillTertiary:'rgba(0,0,0,.07)',fillLight:inherited.fillLight,
 labelPrimaryLight:inherited.labelPrimaryLight,labelPrimary:'#142528',labelSecondary:'#263b3e',
 labelTertiary:'#334b50',labelQuaternary:'#3b5055',lightenHover:inherited.lightenHover,
 lightenPressed:inherited.lightenPressed,error:inherited.error,alert:inherited.alert,passable:inherited.passable,
 interaction:'#d8b989',dividerLine:inherited.dividerLine
}});
const catalogue=[
 {id:'quencher',number:'001',short:'The vessel',title:'Holding more.',subtitle:'A cup that carried more than water.',category:'Ritual',era:'The 2020s',
  description:'A familiar companion for the commute, the desk and the long way home. An everyday vessel became a small declaration of how we wanted to live.',
  question:'When did staying hydrated become a way of belonging?',
  chapters:[['Ritual','A refill marks a pause. Small repeated gestures give a day its shape.'],['Signal','An ordinary object becomes a recognizable badge. What we carry can say who we hope to be.'],['Afterlife','The ritual ends; the object remains. What would you want the next visitor to understand?']]},
 {id:'vacuum',number:'002',short:'The helper',title:'A quieter chore.',subtitle:'A machine learning the shape of a home.',category:'Convenience',era:'The 2020s',
  description:'It traced the same chair legs and thresholds every day. Behind a promise of effortless cleaning was a map of the routines that made a room a home.',
  question:'What could a floor plan tell someone about your life?',
  chapters:[['Routine','The morning route begins beneath a chair. A crumb becomes a record of breakfast.'],['Boundary','Furniture makes islands and doorways make borders. The machine travels through a geography we barely notice.'],['Human trace','The route becomes a portrait. The most ordinary movements leave a pattern worth preserving.']]},
 {id:'smartspeaker',number:'003',short:'The listener',title:'A room, listening.',subtitle:'A small object with an enormous outside world.',category:'Connection',era:'The 2020s',
  description:'Music, a timer, a question spoken into the room. The simplest requests connected domestic space to an infrastructure far beyond its walls.',
  question:'What changes when speaking becomes an interface?',
  chapters:[['Invitation','A voice crosses a room. The object waits at the edge of attention.'],['Connection','One small request opens into a network of distant systems. Convenience makes that distance feel invisible.'],['Silence','When the connection falls quiet, the room returns. Which parts of the ritual did the object really own?']]}
];
const key='everyday-futures-preview-v1';let stored={};
try{stored=JSON.parse(localStorage.getItem(key)||'{}')}catch(_){}
const state={selected:'quencher',view:'catalogue',chapter:0,turn:0,mode:'normal',saved:Array.isArray(stored.saved)?stored.saved.filter(id=>catalogue.some(a=>a.id===id)):[],
 options:{motion:!!stored.options?.motion,transparency:!!stored.options?.transparency,contrast:!!stored.options?.contrast}};
const app=document.getElementById('app');
new ResizeObserver(()=>{document.getElementById('window-anchor').style.height=Math.ceil(app.getBoundingClientRect().height)+'px'}).observe(app);
function persist(){try{localStorage.setItem(key,JSON.stringify({saved:state.saved,options:state.options}))}catch(_){}}
function esc(value){return String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function action(id,text,primary=false,disabled=false){return '<sui-button id="'+id+'" text="'+esc(text)+'" size="regular" '+(primary?'container-color="#d8b989" content-color="#192326"':'container-color="var(--sui-fill-tertiary)" content-color="var(--sui-label-primary)"')+(disabled?' disabled':'')+'></sui-button>'}
function model(id,uid,story=false){
 const defs='<defs><linearGradient id="'+uid+'-body" x1="0" x2="1"><stop stop-color="#71888d"/><stop offset=".33" stop-color="#c3d2ce"/><stop offset=".55" stop-color="#8ca5a5"/><stop offset="1" stop-color="#35565e"/></linearGradient><linearGradient id="'+uid+'-gold"><stop stop-color="#987444"/><stop offset=".48" stop-color="#e9d4a8"/><stop offset="1" stop-color="#a8875d"/></linearGradient><radialGradient id="'+uid+'-shadow"><stop stop-color="#06161f" stop-opacity=".5"/><stop offset="1" stop-color="#06161f" stop-opacity="0"/></radialGradient></defs>';
 let form='';
 if(id==='quencher') form='<ellipse cx="220" cy="332" rx="138" ry="30" fill="url(#'+uid+'-shadow)"/><path d="M279 134 Q351 127 342 207 Q335 249 281 229" fill="none" stroke="#739399" stroke-width="19"/><path d="M279 137 Q333 133 326 204 Q322 229 285 216" fill="none" stroke="#d0ded8" stroke-opacity=".55" stroke-width="4"/><path d="M144 106 L168 294 Q220 328 271 293 L295 106Z" fill="url(#'+uid+'-body)"/><ellipse cx="219" cy="107" rx="76" ry="22" fill="#b4c9c6"/><ellipse cx="219" cy="105" rx="64" ry="14" fill="#304e56"/><path d="M149 112 Q218 135 291 112" fill="none" stroke="#ecede0" stroke-opacity=".6" stroke-width="3"/><path d="M253 24 L240 110" stroke="#dbe5dd" stroke-width="9"/><path d="M254 24 L267 8" stroke="#dbe5dd" stroke-width="9" stroke-linecap="round"/><path d="M181 145 L192 282" stroke="#eaf2e5" stroke-opacity=".38" stroke-width="4"/><ellipse cx="220" cy="299" rx="49" ry="10" fill="#53777c"/><path d="M197 199 Q220 182 242 200" fill="none" stroke="#d8b989" stroke-width="2" opacity=".75"/><circle cx="219" cy="211" r="3" fill="#d8b989"/>';
 if(id==='vacuum') form='<ellipse cx="220" cy="316" rx="154" ry="29" fill="url(#'+uid+'-shadow)"/><path d="M89 230 C89 143 351 143 351 230 L351 263 C339 336 101 336 89 263Z" fill="#527079"/><ellipse cx="220" cy="228" rx="131" ry="80" fill="url(#'+uid+'-body)"/><ellipse cx="220" cy="225" rx="113" ry="65" fill="none" stroke="#e3e9db" stroke-opacity=".45" stroke-width="2"/><ellipse cx="220" cy="188" rx="33" ry="19" fill="#314f57"/><path d="M188 186 L188 172 C188 144 252 144 252 172 L252 186Z" fill="#76999c"/><ellipse cx="220" cy="171" rx="32" ry="17" fill="#c4d4ce"/><circle cx="215" cy="248" r="5" fill="#d8b989"/><path d="M137 280 Q221 320 310 275" fill="none" stroke="#142d35" stroke-width="5"/>';
 if(id==='smartspeaker') form='<ellipse cx="220" cy="332" rx="135" ry="30" fill="url(#'+uid+'-shadow)"/><path d="M128 143 C128 55 310 55 310 143 L310 278 C300 339 138 339 128 278Z" fill="url(#'+uid+'-body)"/><ellipse cx="219" cy="134" rx="91" ry="47" fill="#bfd2cc"/><ellipse cx="219" cy="130" rx="72" ry="32" fill="#58777c"/><path d="M153 137 Q220 107 285 137" stroke="#d8b989" stroke-width="4" fill="none"/>'+Array.from({length:13},(_,i)=>'<path d="M'+(139+i*13)+' 179 L'+(139+i*13)+' 281" stroke="#35565d" stroke-opacity=".34" stroke-width="2"/>').join('')+'<path d="M141 279 Q220 319 298 279" stroke="#dbe4db" stroke-opacity=".3" fill="none"/>';
 let world='';
 if(story){const c=state.chapter;world='<g opacity=".9"><ellipse cx="220" cy="310" rx="192" ry="50" fill="none" stroke="#d8b989" stroke-width="1"/><ellipse cx="220" cy="310" rx="'+(135+c*18)+'" ry="'+(30+c*5)+'" fill="none" stroke="#d8b98966" stroke-dasharray="3 9"/>'+[0,1,2].map((n)=>'<g transform="translate('+(55+n*158)+' '+(292+(n%2)*26)+')"><path d="M-15 0 L0 -9 L15 0 L0 9Z" fill="'+(c===n?'#e7ce9f':'#829895')+'"/><path d="M-15 0 L-15 '+(-18-(n===c?25:0))+' L0 '+(-27-(n===c?25:0))+' L15 '+(-18-(n===c?25:0))+' L15 0 L0 9Z" fill="'+(c===n?'#caaa72':'#576c6e')+'"/><circle cy="'+(-34-(n===c?25:0))+'" r="3" fill="#f4ddae"/></g>').join('')+'</g>';form='<g transform="translate(80 45) scale(.63)">'+form+'</g>';}
 return '<svg viewBox="0 0 440 365" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="'+esc(catalogue.find(a=>a.id===id)?.short||'Artifact')+' — illustrative study">'+defs+world+form+'</svg>';
}
function choose(id){state.selected=id;state.view='catalogue';state.chapter=0;state.turn=0;render()}
function wire(id,fn){document.getElementById(id)?.addEventListener('click-action',fn)}
function close(id){document.getElementById(id).hidden=true}
function show(id){document.getElementById(id).hidden=false}
function render(){
 const a=catalogue.find(x=>x.id===state.selected)||catalogue[0];const isStory=state.view==='story';const saved=state.saved.includes(a.id);
 const header='<header class="topbar"><div class="brand"><div class="monogram" aria-hidden="true">eƒ</div><div><div class="brand-name">Everyday Futures</div><div class="eyebrow">A museum of our habits</div></div></div><div class="top-actions">'+action('notebook-open','Collection · '+state.saved.length)+action('settings-open','Settings')+'</div></header>';
 const collection='<aside class="collection" aria-label="Artifact catalogue"><div class="collection-heading"><div class="eyebrow">The permanent collection</div><h2>Ordinary, extraordinary.</h2></div>'+catalogue.map(x=>'<article class="artifact-card '+(x.id===a.id?'selected':'')+'"><span class="index">'+x.number+'</span><div class="thumb">'+model(x.id,'thumb-'+x.id)+'</div><div class="card-name">'+esc(x.short)+'</div>'+action('select-'+x.id,x.id==='quencher'?'Quencher cup':x.id==='vacuum'?'Robot vacuum':'Smart speaker',x.id===a.id)+'</article>').join('')+'</aside>';
 let center='<section class="stage" aria-label="Artifact inspection"><div class="stage-heading"><span class="eyebrow">'+(isStory?'A small world unfolds':state.view==='inspect'?'An object, up close':'Study no. '+a.number)+'</span><span class="small-status"><span class="status-dot"></span>'+(isStory?'Chapter '+(state.chapter+1)+' / 3':'In the collection')+'</span></div>';
 if(state.mode==='error')center+='<div class="error-panel" id="asset-error"><h2>This object needs a moment.</h2><p>The model could not be opened. Its catalogue entry is still available.</p>'+action('retry-model','Try again',true)+'</div>';
 else center+='<div class="object-field"><div id="hero-model" class="hero-model" style="--turn:'+state.turn+'deg">'+model(a.id,'hero',isStory)+'</div></div><div class="object-footnote"><span>'+a.category+' / '+a.era+'</span><span>'+((state.turn%360+360)%360===0?'Front view':'Turned view')+'</span></div>';
 if(isStory)center+='<div class="chapter-strip">'+a.chapters.map((c,i)=>action('chapter-'+i,c[0],i===state.chapter)).join('')+'</div><p id="story-caption" class="story-caption"><strong>'+esc(a.chapters[state.chapter][0])+'.</strong> '+esc(a.chapters[state.chapter][1])+'</p>';
 else center+='<div class="stage-actions">'+action('rotate-left','↶ Turn left')+action('rotate-right','Turn right ↷')+'</div>';
 center+='</section>';
 const text=state.mode==='fallback'?'This object’s full catalogue note is being restored. You can still inspect the study and explore its story.':a.description;
 const details='<section class="details" aria-label="Artifact details"><div class="object-number">OBJECT / '+a.number+'</div><h1>'+esc(a.title)+'</h1><p class="subtitle">'+esc(a.subtitle)+'</p><p id="description" class="body-copy">'+esc(text)+'</p><p class="tagline">'+esc(a.question)+'</p><dl class="metadata"><div><dt>Collection</dt><dd>Everyday rituals</dd></div><div><dt>Focus</dt><dd>'+a.category+'</dd></div></dl><div class="primary-actions">'+action('story-toggle',isStory?'Return to the object':'Unfold its story',true)+action('inspect-toggle',state.view==='inspect'?'Return to catalogue':'Inspect object',false,isStory)+action('save-object',saved?'Added to your collection':'Keep this object',false,saved)+'</div></section>';
 const empty='<section class="empty-state" id="empty-catalogue"><div class="eyebrow">The collection is resting</div><h1>Every object has a story.</h1><p>The catalogue is not available right now. Please try loading it again.</p>'+action('retry-catalogue','Reload collection',true)+'</section>';
 app.innerHTML=header+'<div class="content-grid" data-view="'+state.view+'">'+(state.mode==='empty'?empty:collection+center+details)+'</div><footer class="footer"><span class="mark">Look closer. Find the human story.</span><span>Private collection<br>Everyday Futures / Study 01</span></footer>';
 wire('notebook-open',()=>{renderNotebook();show('notebook-sheet')});wire('settings-open',()=>show('settings-sheet'));
 catalogue.forEach(x=>wire('select-'+x.id,()=>choose(x.id)));
 wire('rotate-left',()=>{state.turn-=25;state.view='inspect';render()});wire('rotate-right',()=>{state.turn+=25;state.view='inspect';render()});
 wire('inspect-toggle',()=>{state.view=state.view==='inspect'?'catalogue':'inspect';render()});
 wire('story-toggle',()=>{state.view=isStory?'catalogue':'story';state.turn=0;render()});
 a.chapters.forEach((_,i)=>wire('chapter-'+i,()=>{state.chapter=i;render()}));
 wire('save-object',()=>{if(!state.saved.includes(a.id)){state.saved.push(a.id);persist();render()}});
 wire('retry-model',()=>{state.mode='normal';render()});wire('retry-catalogue',()=>{state.mode='normal';render()});
 document.body.classList.toggle('reduce-motion',state.options.motion);document.body.classList.toggle('reduced-transparency',state.options.transparency);document.body.classList.toggle('high-contrast',state.options.contrast);
}
function renderNotebook(){const saved=state.saved.map(id=>catalogue.find(a=>a.id===id)).filter(Boolean);document.getElementById('notebook-content').innerHTML=saved.length?'<ul class="saved-list">'+saved.map(a=>'<li><strong>'+esc(a.title)+'</strong><small>'+esc(a.question)+'</small></li>').join('')+'</ul>':'<p>No objects saved yet. Choose one that tells a story you want to keep.</p>'}
[['reduce-motion','motion'],['reduce-transparency','transparency'],['increase-contrast','contrast']].forEach(([id,key])=>{const el=document.getElementById(id);el.toggleAttribute('checked',state.options[key]);el.addEventListener('checked-change',e=>{state.options[key]=e.detail.checked;persist();render()})});
wire('close-settings',()=>close('settings-sheet'));wire('close-notebook',()=>close('notebook-sheet'));
['settings-sheet','notebook-sheet','reset-dialog'].forEach(id=>document.getElementById(id).addEventListener('dismiss-request',()=>close(id)));
wire('reset-request',()=>{close('settings-sheet');show('reset-dialog')});wire('reset-cancel',()=>close('reset-dialog'));wire('reset-confirm',()=>{state.saved=[];persist();close('reset-dialog');render()});
window.__museumReview={snapshot:()=>JSON.parse(JSON.stringify(state)),setDataMode(mode){if(!['normal','fallback','error','empty'].includes(mode))throw Error('Unknown data mode');state.mode=mode;render()},reset(){state.selected='quencher';state.view='catalogue';state.turn=0;state.chapter=0;state.mode='normal';state.saved=[];state.options={motion:false,transparency:false,contrast:false};persist();for(const id of ['reduce-motion','reduce-transparency','increase-contrast'])document.getElementById(id).removeAttribute('checked');for(const id of ['settings-sheet','notebook-sheet','reset-dialog'])close(id);render()}};
render();
