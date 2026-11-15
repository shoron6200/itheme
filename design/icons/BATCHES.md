# MONO Thin Line — icon batches

Production record for the bundled library (`manifest.json` field `batch`). Settings → Icons shows the same
batches with how many icons of each are in use on the phone.

## Request 2026-11-15 (`requests/mono-missing-icons-2026-11-15.txt`)

- Apps in the request: 198 (all unique, none were already mapped).
- Mapped: 197 — 17 to existing icons, the rest to 107 new icons (batches 3–8).
- Skipped: `com.sndemo.app` ("sndemo") — the app could not be identified, so it keeps its original icon
  rather than a guessed symbol.

Shared-category icons: some apps have no distinctive public mark or are clients of the same service. They
use one category symbol (e.g. `vpn`, `voip`, `sim`, `browser`, `code`, `pay`, `notes`) instead of a fake logo.

## Batch 1 — 20 icons

| Icon | Apps (from request) / packages |
|---|---|
| `whatsapp` | `com.whatsapp`<br>`com.whatsapp.w4b` |
| `telegram` | `org.telegram.messenger`<br>`org.telegram.messenger.web`<br>`org.thunderdog.challegram`<br>exteraGram `com.exteragram.messenger`<br>Fork Client `org.forkclient.messenger.beta`<br>Telegraph `ir.ilmili.telegraph` |
| `chrome` | `com.android.chrome`<br>`com.chrome.beta`<br>`com.chrome.dev`<br>`com.chrome.canary` |
| `youtube` | `com.google.android.youtube`<br>`app.revanced.android.youtube`<br>YT (drifty) `app.dwrv.android.youtube` |
| `spotify` | `com.spotify.music`<br>`com.spotify.lite` |
| `instagram` | `com.instagram.android`<br>`com.instagram.lite` |
| `facebook` | `com.facebook.katana`<br>`com.facebook.lite` |
| `messenger` | `com.facebook.orca`<br>`com.facebook.mlite` |
| `gmail` | `com.google.android.gm`<br>`com.google.android.gm.lite` |
| `maps` | `com.google.android.apps.maps`<br>`com.google.android.apps.mapslite` |
| `photos` | `com.google.android.apps.photos`<br>Photos `app.revanced.android.photos` |
| `camera` | `com.oplus.camera`<br>`com.coloros.camera`<br>`com.android.camera`<br>`com.android.camera2`<br>`com.google.android.GoogleCamera`<br>`com.sec.android.app.camera`<br>`org.codeaurora.snapcam`<br>LMC8.4 `com.camera.lmc84`<br>role: camera |
| `phone` | `com.google.android.dialer`<br>`com.android.dialer`<br>`com.samsung.android.dialer`<br>role: dialer |
| `messages` | `com.google.android.apps.messaging`<br>`com.android.mms`<br>`com.android.messaging`<br>`com.samsung.android.messaging`<br>role: sms |
| `settings` | `com.android.settings`<br>microG Settings `app.revanced.android.gms`<br>role: settings |
| `playstore` | `com.android.vending` |
| `drive` | `com.google.android.apps.docs` |
| `files` | `com.google.android.apps.nbu.files`<br>`com.android.documentsui`<br>`com.coloros.filemanager`<br>`com.oplus.filemanager`<br>`com.sec.android.app.myfiles`<br>`com.mi.android.globalFileexplorer`<br>MiX `com.mixplorer` |
| `calendar` | `com.google.android.calendar`<br>`com.coloros.calendar`<br>`com.android.calendar`<br>`com.samsung.android.calendar`<br>role: calendar |
| `clock` | `com.google.android.deskclock`<br>`com.android.deskclock`<br>`com.coloros.alarmclock`<br>`com.oplus.alarmclock`<br>`com.sec.android.app.clockpackage`<br>Big digital clock display `com.meiyinet.clock`<br>role: clock |

## Batch 2 — 19 icons

| Icon | Apps (from request) / packages |
|---|---|
| `contacts` | `com.google.android.contacts`<br>`com.android.contacts`<br>`com.samsung.android.app.contacts` |
| `calculator` | `com.google.android.calculator`<br>`com.coloros.calculator`<br>`com.oplus.calculator`<br>`com.android.calculator2`<br>`com.sec.android.app.popupcalculator`<br>`com.miui.calculator`<br>CalcES `advanced.scientific.calculator.calc991.plus`<br>DPS Calculator `com.rstechnologiesltd.dpscalculator` |
| `gallery` | `com.coloros.gallery3d`<br>`com.oplus.gallery`<br>`com.android.gallery3d`<br>`com.sec.android.gallery3d`<br>`com.miui.gallery`<br>`com.google.android.apps.photosgo` |
| `music` | `com.heytap.music`<br>`com.oppo.music`<br>`com.android.music`<br>`com.sec.android.app.music`<br>`com.miui.player` |
| `notes` | `com.coloros.note`<br>`com.oplus.note`<br>`com.samsung.android.app.notes`<br>`com.miui.notes`<br>App Note `com.note.todolist.noteapp`<br>ColorNote `com.socialnmobile.dictapps.notepad.color.note`<br>Notes `note.notesapp.notebook.notepad.stickynotes.colornote`<br>StarNote `com.onyx.galaxy.global.note`<br>Tidy Notes `tidynotes.notepad.notes.notebook.note.checklist.todolist`<br>Pocketleaf `app.pocketleaf.notes` |
| `weather` | `com.coloros.weather2`<br>`com.oplus.weather2`<br>`com.sec.android.daemonapp`<br>`com.miui.weather2` |
| `recorder` | `com.coloros.soundrecorder`<br>`com.oplus.soundrecorder`<br>`com.google.android.apps.recorder`<br>`com.android.soundrecorder`<br>`com.sec.android.app.voicenote` |
| `google` | `com.google.android.googlequicksearchbox` |
| `gemini` | `com.google.android.apps.bard` |
| `chatgpt` | `com.openai.chatgpt` |
| `x` | `com.twitter.android` |
| `tiktok` | `com.zhiliaoapp.musically`<br>`com.ss.android.ugc.trill`<br>`com.zhiliaoapp.musically.go` |
| `snapchat` | `com.snapchat.android` |
| `linkedin` | `com.linkedin.android` |
| `netflix` | `com.netflix.mediaclient` |
| `discord` | `com.discord` |
| `keep` | `com.google.android.keep` |
| `meet` | `com.google.android.apps.tachyon`<br>`com.google.android.apps.meetings` |
| `translate` | `com.google.android.apps.translate` |

## Batch 3 — 20 icons

| Icon | Apps (from request) / packages |
|---|---|
| `cloudflare` | 1.1.1.1 `com.cloudflare.onedotonedotonedotone` |
| `download` | 1DM `idm.internet.download.manager`<br>Video Downloader `com.aistudio.videodownloader.zkpxty` |
| `icon_pack` | Adaptive Black `com.hexoline.flatpixelblack`<br>BeeLine `com.alphaone.beeline.iconpack`<br>YellowLine `com.jndapp.limeline.linex.iconpack`<br>Lines Free `com.natewren.linesfree`<br>X Icon Changer `io.hexman.xiconchanger` |
| `scanner` | Adobe Scan `com.adobe.scan.android` |
| `language` | Airlearn `com.unacademy.antonio`<br>Fluently `app.getfluently.app`<br>Pingo `com.picoai.languageapp.android` |
| `voip` | Alaap `com.alaap.app`<br>Fast Call `net.whatscall.freecall`<br>PortSIP UC `com.portgo`<br>Rafusoft Dialer `com.rafusoft.sip`<br>ZoiPer `com.zoiper.android.app`<br>MUTSUDDY `com.vijkar_telecom.mutsuddy` |
| `rewards` | Antamivi `com.axiomc.bocantamivi`<br>Rewards `com.google.android.apps.paidtasks` |
| `authenticator` | Authenticator `com.google.android.apps.authenticator2` |
| `vpn` | Bangladesh Vpn `com.banglatech.bangladeshvpn`<br>Bangladeshi VPN `com.banglamodeapk.banglavpn`<br>ExpressVPN `com.expressvpn.vpn`<br>Ninja Mole `net.mullvad.mullvadvpn`<br>Outline `org.outline.android.client`<br>PandaVPN `com.pandavpn.androidproxy`<br>Socks Proxy `com.Lemulite_Studio.SOCKSIPFREEVPNIP`<br>SocksDroid `net.typeblog.socks`<br>Thunder VPN `com.fast.free.unblock.thunder.vpn`<br>Tun2Socks `com.elseplus.tun2socks`<br>VPN India `com.free.vpn.india.proxy.planet` |
| `bank` | Bank Of Cyprus `cy.com.netinfo.netteller.boc`<br>Citytouch `com.thecitybank.citytouch` |
| `earbuds` | Baseus `com.baseus.intelligent`<br>QCY `com.qcymall.googleearphonesetup` |
| `tag` | Bazaraki `com.larixon.bazaraki` |
| `motorbike` | Bike Race `com.topfreegames.bikeracefreeworld`<br>Traffic Rider `com.skgames.trafficrider`<br>Pathao `com.pathao.user` |
| `binance` | Binance `com.binance.dev` |
| `bkash` | bKash `com.bKash.customerapp` |
| `bolt` | Bolt `ee.mtakso.client`<br>Bolt Nicosia Helper `com.bolthelper.nicosia` |
| `courier` | Bolt Courier `com.bolt.deliverycourier`<br>Wolt Partner `com.wolt.courierapp` |
| `food` | Bolt Food `com.bolt.deliveryclient`<br>Wolt `com.wolt.android` |
| `brave` | Brave `com.brave.browser` |
| `chess` | Chess `com.chess`<br>Pocket Chess `com.dkxqzbfkjt.pocketchess` |

## Batch 4 — 20 icons

| Icon | Apps (from request) / packages |
|---|---|
| `claude` | Claude `com.anthropic.claude` |
| `clone_phone` | Clone Phone `com.coloros.backuprestore` |
| `cloud` | CloudMount `app.cloudmount`<br>HeyTap Cloud `com.heytap.cloud`<br>SN Drive `com.sndrive.com` |
| `streaming` | CloudStream `com.lagradost.cloudstream3`<br>Stremio `com.stremio.one`<br>hoichoi `com.viewlift.hoichoi`<br>Rave `com.wemesh.android` |
| `code` | CodeAssist `com.tyron.code`<br>Expo Go `host.exp.exponent`<br>Jvdroid `ru.iiec.jvdroid`<br>Pydroid 3 `ru.iiec.pydroid3`<br>QuickEdit `com.rhmsoft.edit`<br>TrebEdit `com.teejay.trebedit` |
| `compass` | Compass `com.coloros.compass2`<br>Qibla Compass `qibladirectioncompass.qiblafinder.truenorthcompass` |
| `text_scan` | Copy Paste Any Text On Screen `com.jg.copypasteanytextonphoto` |
| `bus` | Cyprus Bus `com.gmv.cyprusbus2`<br>Pame `app.meep.cyprus.pame` |
| `shopping` | Daraz `com.daraz.android`<br>Temu `com.einnovation.temu` |
| `deepseek` | DeepSeek `com.deepseek.chat` |
| `graph` | Desmos `com.desmos.calculator` |
| `vault` | DocVault `com.os.docvault` |
| `pdf` | E2PDF (Backup & Restore) `com.tekxperiastudios.pdfexporter` |
| `topup` | Easyplan Topup `com.traveltelbd.user` |
| `ai_chip` | Edge Gallery `com.google.ai.edge.gallery`<br>Local Dream `io.github.xororz.localdream` |
| `gps` | Fake GPS Location `com.hopefactory2021.fakegpslocation` |
| `firefox` | Firefox `org.mozilla.firefox` |
| `flashlight` | Flashlight `com.peacock.flashlight` |
| `fonts` | Fonts Art `cool.fonts.symbol.keyboard.custom.fancy.text.editor` |
| `foodpanda` | foodpanda `com.global.foodpanda.android` |

## Batch 5 — 20 icons

| Icon | Apps (from request) / packages |
|---|---|
| `gamepad` | Game Assistant `com.oplus.games` |
| `watch` | GloryFit `com.yc.gloryfit`<br>KW66 Faces `com.mappdev.kw66` |
| `google_one` | Google One `com.google.android.apps.subscriptions.red` |
| `pay` | GPay `com.google.android.apps.nbu.paisa.user`<br>JCCsmart `com.jccsmart`<br>NexusPay `com.dbbl.nexus.pay`<br>RedotPay `com.redotpay`<br>upay `bd.com.upay.customer` |
| `grok` | Grok `ai.x.grok` |
| `checklist` | Habit Tracker - To-Do List `com.rohansaxena.habit_tracker_app`<br>Study Tracker `com.studytracker.app`<br>ListKoro `com.listkoro.app` |
| `google_home` | Home `com.google.android.apps.chromecast.app` |
| `image_search` | Image Search `com.thinkfree.searchbyimage` |
| `mail` | Instant Mail `com.temporary.email.pro` |
| `kde_connect` | KDE Connect `org.kde.kdeconnect_tp` |
| `browser` | Kiwi Browser `com.kiwibrowser.browser`<br>Quetta `net.quetta.browser`<br>Via `mark.via.gp`<br>XBrowser `com.xbrowser.play` |
| `widget` | KWGT `org.kustom.widget` |
| `lens` | Lens `com.google.ar.lens` |
| `localsend` | LocalSend `org.localsend.localsend_app` |
| `heart` | Lovora `com.lovoracards.couplequestion` |
| `launcher` | minimalist phone `com.qqlabs.minimalistlauncher`<br>Nokia 1280 Launcher `com.com.classic.launcheren` |
| `moonpay` | MoonPay `com.moonpay` |
| `patch` | Morphe `app.morphe.manager` |
| `meta` | Muse `com.facebook.aura` |
| `video_player` | MX Player Pro `com.mxtech.videoplayer.pro`<br>Network Stream (Video) Player `com.genuine.leone`<br>Videos `com.coloros.video` |

## Batch 6 — 20 icons

| Icon | Apps (from request) / packages |
|---|---|
| `sim` | My Teletalk `teletalk.teletalkcustomerapp`<br>MyBL `com.arena.banglalinkmela.app`<br>MyCirkle-Airtel `net.omobio.airtelsc`<br>MyGP `com.portonics.mygp`<br>MyPrimetel `primetel.androidapp.com.primetel`<br>PrimeTel Menu `com.android.stk`<br>ORoaming `com.redteamobile.roaming` |
| `money` | MyMoney `com.raha.app.mymoney.free` |
| `fuel` | myPetrolina `com.petrolina.loyaltyapp` |
| `nagad` | Nagad `com.konasl.nagad` |
| `news` | News `com.google.android.apps.magazines`<br>The Daily Ittefaq `org.chromium.webapk.a522ce88e3872f219_v2` |
| `nordvpn` | NordVPN `com.nordvpn.android` |
| `printer` | NokoPrint `com.nokoprint` |
| `notebook` | Notebook `com.google.android.apps.labs.language.tailwind` |
| `notion` | Notion `notion.id` |
| `outlook` | Outlook `com.microsoft.office.outlook` |
| `circuit` | P R O T O `com.proto.circuitsimulator` |
| `paypal` | PayPal `com.paypal.android.p2pmobile` |
| `phone_manager` | Phone Manager `com.coloros.phonemanager` |
| `book` | Bangla Dictionary `com.hdictionary.bn`<br>Play Books `com.google.android.apps.books`<br>হুমায়ুন আহমেদ `com.humayunahmed.shomagro` |
| `flashcards` | Quizlet `com.quizlet.quizletandroid` |
| `quora` | Quora `com.quora.android` |
| `link` | realme Link `com.realme.link` |
| `reddit` | Reddit `com.reddit.frontpage` |
| `revolut` | Revolut `com.revolut.revolut` |
| `keyboard` | Ridmik `ridmik.keyboard` |

## Batch 7 — 20 icons

| Icon | Apps (from request) / packages |
|---|---|
| `rocket` | Rocket `com.dbbl.mbs.apps.main` |
| `remote_desktop` | RustDesk `com.carriez.flutter_hbb`<br>RVNC Viewer `com.realvnc.viewer.android`<br>Windows App `com.microsoft.rdc.androidx` |
| `secure_chat` | SafeUM `com.safeum.android` |
| `lock` | Screen Lock `com.coloros.onekeylockscreen` |
| `shazam` | Shazam `com.shazam.android` |
| `shortcut` | Shortcut Maker `rk.android.app.shortcutmaker` |
| `sketch` | Sketchbook `com.adsk.sketchbook` |
| `send_money` | Taptap Send `com.taptapsend` |
| `tasks` | Tasks `com.google.android.apps.tasks` |
| `tv` | Televizo `com.ottplay.ottplay` |
| `repeat` | Text Repeater `com.centroidapps.textrepeater` |
| `themes` | Theme Store `com.oplus.themestore` |
| `threads` | Threads `com.instagram.barcelona` |
| `ton` | Tonkeeper `com.ton_keeper` |
| `trading` | Trading 212 `com.avuscapital.trading212`<br>TradingView `com.tradingview.tradingviewapp` |
| `truecaller` | Truecaller `com.truecaller` |
| `car` | Uber `com.ubercab` |
| `wallet` | Wallet `com.google.android.apps.walletnfcrel` |
| `waze` | Waze `com.waze` |
| `vlc` | VLC `org.videolan.vlc` |

## Batch 8 — 7 icons

| Icon | Apps (from request) / packages |
|---|---|
| `office` | WPS Office `cn.wps.moffice_eng` |
| `zen` | Zen Space `com.oneplus.brickmode` |
| `network` | ZeroTier One `com.zerotier.one`<br>Reqable `com.reqable.android` |
| `airplane` | Qatar Airways `com.m.qr` |
| `wallpaper` | LiveLoop `com.wallpaper.liveloop` |
| `ai_chat` | Poe `com.poe.android`<br>Private AI `us.valkon.privateai`<br>PrivateAgent `com.orailnoor.privateagent` |
| `duckduckgo` | DuckDuckGo `com.duckduckgo.mobile.android` |
