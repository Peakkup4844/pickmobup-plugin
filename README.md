# PickMobUp

ปลั๊กอิน Minecraft สำหรับ **อุ้ม entity ขึ้นไว้บนหัว** แล้ว **โยนออกไปแบบสลิงช็อต**

รองรับ **Spigot / Paper / Folia** ตั้งแต่ **1.20.1** ถึงเวอร์ชันล่าสุด (ทดสอบ build กับ 1.21.x)

## วิธีเล่น

| การกระทำ | ผล |
|---|---|
| **ย่อ (Sneak) + คลิกขวา** ที่ entity | อุ้มขึ้นมาไว้บนหัว |
| **แตะ Shift สั้น ๆ** (ขณะอุ้ม) | วาง entity ลงตรงตำแหน่งที่ยืน |
| **กด Shift ค้าง** (ขณะอุ้ม) | ชาร์จพลัง — หลอดวิ่งขึ้น-ลงบน actionbar |
| **ปล่อย Shift** (ขณะชาร์จ) | โยน entity ไปทางที่เล็ง แรงตามค่าหลอด ณ จังหวะปล่อย |

ระหว่าง entity ลอยอยู่จะติดสถานะ **Slow Falling** จนกว่าจะแตะพื้น

## คำสั่ง / สิทธิ์

- `/pmu reload` — รีโหลด config และ lang (`pickmobup.admin`)
- `pickmobup.use` — อุ้ม/โยนได้ (default: true)
- `pickmobup.carryplayers` — อุ้มผู้เล่นได้ (default: op)

## การตั้งค่า

- `config.yml` — โหมดอุ้ม, white/blacklist ชนิด entity, อุ้มผู้เล่นได้หรือไม่, โลกที่อนุญาต,
  แรงโยนสูงสุด, ความเร็ว, เสียง ฯลฯ
- `lang.yml` — ข้อความทั้งหมด (รหัสสี `&`)

### โหมดอุ้ม (`mount-mode`)

- `PASSENGER` *(แนะนำ)* — ใช้กลไก vanilla ไม่ต้องพึ่งปลั๊กอินเสริม
- `PACKET` — แสดงผลด้วย packet (ต้องติดตั้งปลั๊กอิน **PacketEvents**)
  หากไม่พบ PacketEvents จะ fallback กลับไปใช้ `PASSENGER` ให้อัตโนมัติ

## การ build

```bash
./gradlew build      # Linux / macOS
gradlew.bat build    # Windows
```

ไฟล์ผลลัพธ์อยู่ที่ `build/libs/PickMobUp-<version>.jar` (shaded แล้ว นำไปวางใน `plugins/` ได้เลย)

## หมายเหตุทางเทคนิค

- คอมไพล์เป็น bytecode **Java 17** จึงรันได้ทั้งบน 1.20.1 (Java 17) และ 1.21.x (Java 21+)
- ใช้ **FoliaLib** (shade + relocate) เพื่อสลับ scheduler ระหว่าง Bukkit และ Folia region threads อัตโนมัติ
- actionbar ส่งผ่าน BungeeCord Chat API ที่มีอยู่ทั้งบน Spigot และ Paper
