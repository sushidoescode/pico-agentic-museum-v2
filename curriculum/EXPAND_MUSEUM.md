# Add the rest of the museum

Use this guide with [Lesson 5](../lessons/L4.md). You will add fifteen objects, small pictures for the collection and the archive room. Keep the three models already working in your app.

## Download these files

Open the links below and download the files to your computer. If Drive gives you a ZIP, unpack it first.

| Download | What to take |
| --- | --- |
| [Exhibit models](https://drive.google.com/drive/folders/1ZHyUQuv7WBgXgxFQfopJNDOczROMhnFr) | `vessel_study.glb` is in this folder. The other seventeen `.glb` files are inside `exhibits`. You already used three models in Lesson 2; the table below identifies the remaining fifteen. |
| [Small collection pictures](https://drive.google.com/drive/folders/1CFoOWyFcZfz59ex5rcAj90XkCyjHkuHa) | All eighteen `.png` files. These are the small pictures shown beside the object names. |
| [Room for the app](https://drive.google.com/drive/folders/1opAUBIbR5VZWLGBYDfXpSLYBjI7C2Y9K) | `earth_archive.glb` and `room-layout.json`. The first is the room model; the second describes where the exhibits and viewing points go. |

If you enter through the [main asset folder](../ASSET_SUPPLEMENT.md), these downloads are under `project/native/app/src/main/assets/`, in `models`, `thumbnails` and `room`. You do not need Blender to use the supplied room model.

## Copy them into your working app

Open your project's `app/src/main/assets` folder. Create these folders if they are missing:

- **`models`** — put the fifteen new model files directly here, beside the three from Lesson 2. Take the files out of Drive's `exhibits` folder; do not copy that folder itself. Keep the three working files where they are.
- **`thumbnails`** — put all eighteen picture files here, keeping their names.
- **`room`** — put `earth_archive.glb` and `room-layout.json` here.

For example, the coffee pod becomes `app/src/main/assets/models/coffeepod.glb`, its picture becomes `app/src/main/assets/thumbnails/coffeepod.png`, and the room becomes `app/src/main/assets/room/earth_archive.glb`.

These are the locations for your **new project**. Share them with your assistant; they need not match the old museum's internal folders. If your earlier models are somewhere else, keep those working locations and tell the assistant what they are.

## Match each object to its files

The first three rows are the objects you already added. Start the new batch with Coffee pod, Delivery box and Disposable face mask.

| Object | File inside your `models` folder | File inside your `thumbnails` folder |
| --- | --- | --- |
| Handled tumbler | `vessel_study.glb` | `quencher.png` |
| Robot vacuum | `vacuum.glb` | `vacuum.png` |
| Smart speaker | `smartspeaker.glb` | `smartspeaker.png` |
| Coffee pod | `coffeepod.glb` | `coffeepod.png` |
| Delivery box | `deliverybox.glb` | `deliverybox.png` |
| QR menu stand | `qrcode.glb` | `qrcode.png` |
| Fidget spinner | `fidget.glb` | `fidget.png` |
| Disposable face mask | `facemask.glb` | `facemask.png` |
| Smartphone | `smartphone.glb` | `smartphone.png` |
| Wireless earbuds and case | `earbuds.glb` | `earbuds.png` |
| Smartwatch | `smartwatch.glb` | `smartwatch.png` |
| Hardware crypto wallet | `cryptowallet.glb` | `cryptowallet.png` |
| Streaming remote | `remote.glb` | `remote.png` |
| Wi-Fi router | `router.glb` | `router.png` |
| Doorbell camera | `doorbell.glb` | `doorbell.png` |
| E-scooter | `scooter.glb` | `scooter.png` |
| Plastic gas can | `gascan.glb` | `gascan.png` |
| Saturn V desk replica | `rocket.glb` | `rocket.png` |

## Copy the collection words

Copy only the rows for your current batch into ChatGPT. These names, titles, sections and identification descriptions are copied exactly from the [frozen museum collection](../source/snapshots/L4-complete/native/app/src/main/java/dev/local/everydayfutures/museum/catalogue/Catalogue.kt). The original also has fictional curator notes; the descriptions below are its separate identification text.

| Object name | Title | Collection section | Description |
| --- | --- | --- | --- |
| Handled tumbler | Holding more. | Daily rituals | A large vacuum-insulated stainless-steel tumbler with a handle, a lid and a reusable straw. Insulated drinkware like this became a widespread everyday accessory, and something people collected, in the early 2020s. |
| Robot vacuum | A quieter chore. | Domestic systems and movement | A battery-powered vacuum cleaner that steers itself around a floor using bump and cliff sensors and, in many models, laser or camera mapping, then returns to a dock to recharge. Robot vacuums became common household appliances during the 2010s. |
| Smart speaker | A room, listening. | Connected lives | A voice-controlled speaker whose microphones listen for a wake word and pass requests to an online assistant for music, timers, questions and home control. Smart speakers entered millions of homes from the mid-2010s. |
| Coffee pod | One cup at a time. | Daily rituals | A single-serve capsule of ground coffee sealed with foil, brewed by a machine that pierces it. Pod brewers spread through homes and offices in the 2000s and 2010s; most capsules were used once and thrown away. |
| Delivery box | Arrived at the door. | Daily rituals | A corrugated cardboard shipping carton sealed with tape. Online shopping made home parcel deliveries routine in the 2010s and 2020s; billions of boxes like this were shipped every year, and most were recycled or discarded within days. |
| QR menu stand | Scan to read. | Daily rituals | A small tabletop stand printed with a QR code, a two-dimensional barcode first developed in 1994. Restaurants adopted code-linked menus widely in 2020 and 2021 to avoid shared paper menus during the COVID-19 pandemic. The code on this exhibit is decorative and links nowhere. |
| Fidget spinner | Something to turn. | Daily rituals | A small three-lobed toy that spins on a central ball bearing, held between finger and thumb. It became a worldwide craze in 2017 and was sold as a way to occupy restless hands. |
| Disposable face mask | A shared precaution. | Daily rituals | A pleated three-layer disposable mask with elastic ear loops. During the COVID-19 pandemic, from 2020, masks like this were worn daily by very large numbers of people and were required in many public places. |
| Smartphone | Everything, in one hand. | Connected lives | A touchscreen mobile phone that combines calls, camera, maps, messaging, payments and installable apps. Touchscreen smartphones spread rapidly after 2007; by the 2020s billions of people carried one. |
| Wireless earbuds and case | A private soundtrack. | Connected lives | A pair of cordless in-ear headphones that connect by Bluetooth, kept in a pocket case whose battery recharges them. Fully wireless earbuds became common from the late 2010s. |
| Smartwatch | Counting the body. | Connected lives | A wrist-worn computer with a touchscreen that shows the time and notifications and records movement and heart rate with built-in sensors. Smartwatches and fitness trackers became widespread in the 2010s. |
| Hardware crypto wallet | A key in the pocket. | Connected lives | A small USB device that keeps the private keys for cryptocurrency accounts offline and signs transactions when connected to a computer. Hardware wallets appeared in the mid-2010s as digital currencies such as Bitcoin, launched in 2009, grew in value. |
| Streaming remote | Choosing what's next. | Connected lives | A compact remote for a television streaming device, with a direction pad, a few playback buttons and often a microphone for voice search. On-demand streaming became one of the main ways to watch television in the 2010s. |
| Wi-Fi router | The invisible room. | Domestic systems and movement | A home networking device that connects a household to the internet and shares the connection by radio using the Wi-Fi standards, first published in 1997. By the 2010s most connected homes relied on one. |
| Doorbell camera | Who's at the door? | Domestic systems and movement | A doorbell with a built-in camera, microphone and speaker that sends video to a phone when someone approaches or presses the button. Video doorbells became popular from the mid-2010s. |
| E-scooter | The last mile. | Domestic systems and movement | A standing electric kick scooter with a battery in its deck and a motor in one wheel. Shared rental scooters unlocked by phone appeared in many cities from 2017, alongside privately owned folding models. |
| Plastic gas can | A portable reserve. | Domestic systems and movement | A moulded plastic container with a handle and pouring spout for carrying and storing petrol (gasoline); red was a common colour for fuel cans. Households kept one for mowers, generators and stranded cars. |
| Saturn V desk replica | An old ambition on the desk. | Domestic systems and movement | A desk-sized scale model of the Saturn V, the rocket that launched the Apollo missions, including the first crewed Moon landing in 1969; it last flew in 1973. Replicas like this were made and collected long after the real rockets retired. |

## Add one batch, then try it

Return to [Add a few objects](../lessons/L4.md#add-a-few-objects). Share the three rows you need, the model and picture locations, and your latest app code. After applying the change, build and open your app. Choose each new object and check that its name, picture and 3D model agree before adding another batch.

## Bring in the room

Check that your project contains both `app/src/main/assets/room/earth_archive.glb` and `app/src/main/assets/room/room-layout.json`. Open the JSON file as text and paste its contents into your conversation, together with the room model's location. You do not need to understand every number; the layout tells the assistant where the eighteen named exhibits and three viewing areas belong.

Use [Enter the room](../lessons/L4.md#enter-the-room) to start with one exhibit and a way back. Once you can enter and return, add the other exhibits. Copying these files alone does not add the room button or make the objects appear; those are the changes you ask for in the lesson.

The download paths follow the supplement's `RESTORE_MAP.json`; filenames also match the [asset register](../assets/ASSET_REGISTER.json). The layout is available as a [saved copy here](../source/snapshots/L4-complete/native/app/src/main/assets/room/room-layout.json). Use the art under the conditions in the [asset notes](../assets/PROVENANCE_AND_RIGHTS.md).
