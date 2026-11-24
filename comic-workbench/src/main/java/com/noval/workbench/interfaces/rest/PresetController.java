package com.noval.workbench.interfaces.rest;

import com.noval.workbench.application.PresetService;
import com.noval.workbench.domain.model.Preset;
import com.noval.workbench.domain.model.PresetItem;
import com.noval.workbench.domain.model.PresetType;
import com.noval.workbench.domain.model.Novel;
import com.noval.workbench.domain.repository.NovelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 预设控制器
 */
@RestController
@RequestMapping("/api/presets")
public class PresetController {

    private final PresetService presetService;
    private final NovelRepository novelRepository;

    public PresetController(PresetService presetService, NovelRepository novelRepository) {
        this.presetService = presetService;
        this.novelRepository = novelRepository;
    }

    @GetMapping
    public ResponseEntity<?> getByNovel(@RequestParam Long novelId) {
        return presetService.getByNovelId(novelId)
                .map(p -> ResponseEntity.ok((Object) toMap(p)))
                .orElse(ResponseEntity.ok((Object) new HashMap<>()));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody Map<String, Object> body) {
        Long novelId = ((Number) body.get("novelId")).longValue();
        String plotSummary = (String) body.get("plotSummary");
        List<PresetItem> characters = parseItems((List<?>) body.get("characters"));
        List<PresetItem> scenes = parseItems((List<?>) body.get("scenes"));
        List<PresetItem> props = parseItems((List<?>) body.get("props"));

        Optional<Preset> existing = presetService.getByNovelId(novelId);
        Preset preset = new Preset.Builder()
                .id(existing.map(Preset::getId).orElse(null))
                .novelId(novelId)
                .plotSummary(plotSummary)
                .characters(characters)
                .scenes(scenes)
                .props(props)
                .build();

        Preset saved = presetService.save(preset);
        return ResponseEntity.ok((Object) toMap(saved));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam Long novelId) {
        presetService.deleteByNovelId(novelId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 智能生成预设（角色/场景/道具）
     * POST /api/presets/smart?novelId=123&type=character
     * 返回: { "items": [{"name":"...","description":"...","type":"...","imageUrl":""}] }
     */
    @PostMapping("/smart")
    public ResponseEntity<?> smartGenerate(@RequestParam Long novelId, @RequestParam String type) {
        PresetType presetType = PresetType.fromString(type);
        if (presetType == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "非法类型: " + type));
        }
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (!novelOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "小说不存在"));
        }
        Novel novel = novelOpt.get();
        String content = novel.getContent();
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.singletonMap("items", Collections.emptyList()));
        }

        List<PresetItem> items = generateSmartPreset(content, presetType);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items.stream().map(PresetItem::toJson).collect(Collectors.toList()));
        return ResponseEntity.ok(result);
    }

    /** 常见中文姓氏（百家姓前 120，用于人名识别过滤） */
    private static final Set<String> SURNAMES = new LinkedHashSet<>(Arrays.asList(
            "赵","钱","孙","李","周","吴","郑","王","冯","陈","褚","卫","蒋","沈","韩","杨",
            "朱","秦","尤","许","何","吕","施","张","孔","曹","严","华","金","魏","陶","姜",
            "戚","谢","邹","喻","柏","水","窦","章","云","苏","潘","葛","奚","范","彭","郎",
            "鲁","韦","昌","马","苗","凤","花","方","俞","任","袁","柳","酆","鲍","史","唐",
            "费","廉","岑","薛","雷","贺","倪","汤","滕","殷","罗","毕","郝","邬","安","常",
            "乐","于","时","傅","皮","卞","齐","康","伍","余","元","卜","顾","孟","平","黄",
            "和","穆","萧","尹","姚","邵","湛","汪","祁","毛","禹","狄","米","贝","明","臧",
            "计","伏","成","戴","谈","宋","茅","庞","熊","纪","舒","屈","项","祝","董","梁",
            "杜","阮","蓝","闵","席","季","麻","强","贾","路","娄","危","江","童","颜","郭",
            "梅","盛","林","刁","钟","徐","邱","骆","高","夏","蔡","田","樊","胡","凌","霍",
            "虞","万","支","柯","管","卢","莫","房","裘","缪","解","应","宗","丁","宣","邓",
            "郁","单","杭","洪","包","诸","左","石","崔","吉","龚","程","嵇","邢","滑","裴",
            "陆","荣","翁","荀","羊","惠","甄","曲","家","封","芮","靳","段","富","巫","乌",
            "焦","巴","弓","牧","隗","山","谷","车","侯","全","班","仰","秋","仲","伊","宫",
            "宁","仇","栾","暴","甘","厉","戎","祖","武","符","刘","景","詹","束","龙","叶",
            "幸","司","韶","黎","蓟","薄","印","宿","白","怀","蒲","从","鄂","索","咸","赖",
            "卓","蔺","屠","蒙","池","乔","阴","胥","能","苍","闻","莘","党","翟","谭","贡",
            "劳","姬","申","扶","堵","冉","宰","郦","雍","桑","桂","牛","寿","通","边","燕",
            "冀","浦","尚","农","温","别","庄","晏","柴","瞿","阎","充","慕","连","茹","习",
            "宦","艾","鱼","容","向","古","易","慎","戈","廖","庾","终","居","衡","步","满"
    ));

    /** 常见含姓氏但非人名的词，避免误判 */
    private static final Set<String> NAME_BLACKLIST = new LinkedHashSet<>(Arrays.asList(
            "黄金","白云","江山","夏日","秋天","黄昏","高兴","千万","马上","华人","龙头",
            "雷同","安静","常常","乐意","于是","时间","康复","毛病","许多","何处","金刚",
            "周日","文中","李子","花开","月明","方便","任意","袁泉","安全","成语"
    ));

    /**
     * 基于小说内容智能生成预设条目（规则提取，可后续替换为 LLM）
     */
    private List<PresetItem> generateSmartPreset(String content, PresetType type) {
        List<PresetItem> items = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");

        if (type == PresetType.CHARACTER) {
            Set<String> seen = new LinkedHashSet<>();
            java.util.regex.Pattern wordPattern = java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]{2,3}");
            for (String line : lines) {
                if (items.size() >= 10) break;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                java.util.regex.Matcher m = wordPattern.matcher(trimmed);
                while (m.find()) {
                    String word = m.group();
                    String surname = word.substring(0, 1);
                    if (!SURNAMES.contains(surname)) continue;
                    if (NAME_BLACKLIST.contains(word)) continue;
                    if (seen.contains(word)) continue;
                    seen.add(word);
                    // 描述取包含该词的原句（截断）
                    String desc = trimmed.length() > 60 ? trimmed.substring(0, 60) + "…" : trimmed;
                    items.add(new PresetItem(word, desc, PresetType.CHARACTER, null));
                    if (items.size() >= 10) break;
                }
            }
        } else if (type == PresetType.SCENE) {
            String[] sceneKeywords = {"室内","室外","房间","大厅","街道","花园","森林","山","河","湖","海","城","村","殿","堂","楼","阁","寺","庙","厨房","卧室","客厅","办公室","餐厅","公园","酒店","酒吧","学校","医院"};
            Set<String> seen = new LinkedHashSet<>();
            for (String line : lines) {
                if (items.size() >= 10) break;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                for (String kw : sceneKeywords) {
                    if (trimmed.contains(kw) && !seen.contains(kw)) {
                        seen.add(kw);
                        String desc = trimmed.length() > 60 ? trimmed.substring(0, 60) + "…" : trimmed;
                        items.add(new PresetItem(kw, desc, PresetType.SCENE, null));
                        break;
                    }
                }
            }
        } else if (type == PresetType.PROP) {
            String[] propKeywords = {"剑","刀","枪","书","信","玉","珠","宝","药","酒","茶","花","灯","镜","琴","棋","画","戒指","项链","手机","钥匙","照片","文件","红包","银行卡","口红","香水","伞"};
            Set<String> seen = new LinkedHashSet<>();
            for (String line : lines) {
                if (items.size() >= 10) break;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                for (String kw : propKeywords) {
                    if (trimmed.contains(kw) && !seen.contains(kw)) {
                        seen.add(kw);
                        String desc = trimmed.length() > 60 ? trimmed.substring(0, 60) + "…" : trimmed;
                        items.add(new PresetItem(kw, desc, PresetType.PROP, null));
                        break;
                    }
                }
            }
        }
        return items;
    }

    private List<PresetItem> parseItems(List<?> raw) {
        if (raw == null) return new ArrayList<>();
        List<PresetItem> items = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Map) {
                items.add(PresetItem.fromJson((Map<String, Object>) o));
            }
        }
        return items;
    }

    private Map<String, Object> toMap(Preset preset) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", preset.getId());
        map.put("novelId", preset.getNovelId());
        map.put("plotSummary", preset.getPlotSummary());
        map.put("characters", preset.getCharacters().stream().map(PresetItem::toJson).collect(Collectors.toList()));
        map.put("scenes", preset.getScenes().stream().map(PresetItem::toJson).collect(Collectors.toList()));
        map.put("props", preset.getProps().stream().map(PresetItem::toJson).collect(Collectors.toList()));
        return map;
    }
}
