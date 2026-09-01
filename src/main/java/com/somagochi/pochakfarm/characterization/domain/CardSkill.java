package com.somagochi.pochakfarm.characterization.domain;

import java.util.Arrays;
import java.util.List;

public enum CardSkill {
  GROUND_PAW_STRIKE(
      CardType.GROUND, SkillBattleType.GAMBLE, "냥냥 펀치", "빠르고 날카로운 펀치로 상대를 공격해요.", "paw"),
  GROUND_LEAF_GUARD(
      CardType.GROUND, SkillBattleType.STABLE, "나뭇잎 방어", "싱그러운 잎사귀로 몸을 감싸 피해를 줄여요.", "leaf"),
  GROUND_SUNNY_NAP(
      CardType.GROUND, SkillBattleType.STABLE, "햇살 낮잠", "따뜻한 햇살 아래 쉬며 기운을 회복해요.", "sun"),
  GROUND_ROOT_BIND(
      CardType.GROUND, SkillBattleType.BALANCED, "뿌리 묶기", "땅속 뿌리로 상대의 발을 붙잡아요.", "leaf"),
  GROUND_MUD_DASH(CardType.GROUND, SkillBattleType.GAMBLE, "진흙 돌진", "흙먼지를 일으키며 힘차게 돌진해요.", "paw"),
  GROUND_ACORN_SHOT(
      CardType.GROUND, SkillBattleType.GAMBLE, "도토리 슛", "작은 도토리를 빠르게 날려 견제해요.", "leaf"),
  GROUND_FOREST_HIDE(
      CardType.GROUND, SkillBattleType.STABLE, "숲속 숨기", "나무 그림자에 숨어 다음 기회를 노려요.", "leaf"),
  GROUND_GRASS_ROLL(
      CardType.GROUND, SkillBattleType.BALANCED, "풀밭 구르기", "풀밭을 굴러 몸놀림을 가볍게 만들어요.", "paw"),
  GROUND_BLOSSOM_HEAL(
      CardType.GROUND, SkillBattleType.STABLE, "꽃잎 회복", "향긋한 꽃잎 기운으로 상처를 달래요.", "leaf"),
  GROUND_STONE_TAP(
      CardType.GROUND, SkillBattleType.BALANCED, "조약돌 톡톡", "작은 조약돌을 튕겨 빈틈을 만들어요.", "paw"),
  GROUND_MOSS_CUSHION(
      CardType.GROUND, SkillBattleType.STABLE, "이끼 쿠션", "폭신한 이끼로 충격을 부드럽게 받아요.", "leaf"),
  GROUND_VINE_PULL(
      CardType.GROUND, SkillBattleType.BALANCED, "덩굴 당기기", "초록 덩굴로 상대를 가까이 끌어와요.", "leaf"),
  GROUND_HARVEST_BOOST(
      CardType.GROUND, SkillBattleType.BALANCED, "수확 기운", "풍성한 자연의 힘으로 파워를 높여요.", "sun"),
  GROUND_TAIL_SEED(
      CardType.GROUND, SkillBattleType.BALANCED, "꼬리 씨앗", "꼬리 끝에서 씨앗을 톡톡 뿌려요.", "leaf"),
  GROUND_EARTH_SHAKE(CardType.GROUND, SkillBattleType.GAMBLE, "땅울림", "발을 굴러 주변을 살짝 흔들어요.", "paw"),
  GROUND_BARK_SHIELD(
      CardType.GROUND, SkillBattleType.STABLE, "나무껍질 방패", "단단한 나무껍질처럼 몸을 지켜요.", "leaf"),
  GROUND_CLOVER_LUCK(
      CardType.GROUND, SkillBattleType.BALANCED, "클로버 행운", "네잎클로버 기운으로 좋은 흐름을 불러요.", "leaf"),
  GROUND_POLLEN_SPARK(
      CardType.GROUND, SkillBattleType.BALANCED, "꽃가루 반짝", "반짝이는 꽃가루로 시선을 흐려요.", "leaf"),
  GROUND_HILL_JUMP(CardType.GROUND, SkillBattleType.GAMBLE, "언덕 점프", "낮은 언덕을 박차고 높이 뛰어올라요.", "paw"),
  GROUND_WILD_SNIFF(
      CardType.GROUND, SkillBattleType.BALANCED, "야생 킁킁", "숲의 냄새를 맡아 길을 찾아내요.", "paw"),
  GROUND_SOIL_CHARGE(
      CardType.GROUND, SkillBattleType.GAMBLE, "흙먼지 차지", "흙먼지를 두르고 힘을 모아 달려요.", "paw"),
  GROUND_LEAF_SLASH(
      CardType.GROUND, SkillBattleType.GAMBLE, "잎새 베기", "날카로운 잎새 바람으로 빠르게 베어요.", "leaf"),
  GROUND_GARDEN_REST(
      CardType.GROUND, SkillBattleType.STABLE, "정원 휴식", "평화로운 정원에서 마음을 가다듬어요.", "leaf"),
  GROUND_BERRY_BITE(
      CardType.GROUND, SkillBattleType.GAMBLE, "산딸기 깨물기", "상큼한 산딸기 기운으로 활력을 얻어요.", "paw"),
  GROUND_LOG_ROLL(
      CardType.GROUND, SkillBattleType.GAMBLE, "통나무 굴리기", "통나무를 굴려 상대의 진로를 막아요.", "paw"),
  GROUND_SPROUT_STEP(
      CardType.GROUND, SkillBattleType.BALANCED, "새싹 발걸음", "새싹처럼 가볍게 움직이며 균형을 잡아요.", "leaf"),
  GROUND_NATURE_CALL(
      CardType.GROUND, SkillBattleType.STABLE, "자연의 부름", "숲의 기운을 불러 주변을 안정시켜요.", "leaf"),
  GROUND_MOUNTAIN_POSE(
      CardType.GROUND, SkillBattleType.STABLE, "산봉우리 자세", "산처럼 단단한 자세로 버텨내요.", "paw"),
  GROUND_HONEY_TRAP(
      CardType.GROUND, SkillBattleType.STABLE, "꿀향 함정", "달콤한 향기로 상대의 집중을 흐려요.", "leaf"),
  GROUND_GREEN_FLASH(
      CardType.GROUND, SkillBattleType.GAMBLE, "초록 섬광", "초록빛을 번쩍이며 재빠르게 파고들어요.", "leaf"),

  SKY_CLOUD_JUMP(CardType.SKY, SkillBattleType.GAMBLE, "구름 점프", "폭신한 구름을 밟고 높이 뛰어올라요.", "cloud"),
  SKY_WIND_DASH(CardType.SKY, SkillBattleType.GAMBLE, "바람 돌진", "상쾌한 바람을 타고 빠르게 돌진해요.", "wind"),
  SKY_FEATHER_GUARD(CardType.SKY, SkillBattleType.STABLE, "깃털 방어", "가벼운 깃털 장막으로 공격을 흘려요.", "cloud"),
  SKY_SUNBEAM(CardType.SKY, SkillBattleType.GAMBLE, "햇살 광선", "밝은 햇살을 모아 따뜻한 빛을 쏘아요.", "sun"),
  SKY_FLOATING_STEP(CardType.SKY, SkillBattleType.BALANCED, "둥실 걸음", "몸을 둥실 띄워 가볍게 이동해요.", "cloud"),
  SKY_BREEZE_HEAL(CardType.SKY, SkillBattleType.STABLE, "산들 회복", "부드러운 산들바람으로 기운을 회복해요.", "wind"),
  SKY_RAINBOW_TAIL(
      CardType.SKY, SkillBattleType.BALANCED, "무지개 꼬리", "무지개빛 꼬리 흔들기로 분위기를 바꿔요.", "cloud"),
  SKY_AIR_SPIN(CardType.SKY, SkillBattleType.STABLE, "공중 회전", "공중에서 빙글 돌아 공격을 피해요.", "wind"),
  SKY_SKY_PAW(CardType.SKY, SkillBattleType.GAMBLE, "하늘 발톱", "하늘 기운을 담은 발톱으로 할퀴어요.", "paw"),
  SKY_CLOUD_HIDE(CardType.SKY, SkillBattleType.STABLE, "구름 숨기", "구름 사이에 숨어 모습을 감춰요.", "cloud"),
  SKY_GUST_PUSH(CardType.SKY, SkillBattleType.GAMBLE, "돌풍 밀기", "갑작스러운 돌풍으로 상대를 밀어내요.", "wind"),
  SKY_LIGHT_WING(CardType.SKY, SkillBattleType.STABLE, "빛날개", "빛나는 날개 기운으로 몸을 가볍게 해요.", "cloud"),
  SKY_STAR_BALLOON(CardType.SKY, SkillBattleType.BALANCED, "별풍선", "반짝이는 풍선을 띄워 시선을 끌어요.", "cloud"),
  SKY_BLUE_FLASH(CardType.SKY, SkillBattleType.GAMBLE, "푸른 섬광", "맑은 하늘빛으로 순간적으로 돌파해요.", "wind"),
  SKY_HIGH_NOSE(CardType.SKY, SkillBattleType.BALANCED, "높은 킁킁", "높은 공기의 냄새로 방향을 찾아요.", "paw"),
  SKY_SOFT_LANDING(CardType.SKY, SkillBattleType.STABLE, "폭신 착지", "구름처럼 부드럽게 내려앉아요.", "cloud"),
  SKY_WIND_CHIME(CardType.SKY, SkillBattleType.BALANCED, "바람종", "맑은 바람종 소리로 마음을 흔들어요.", "wind"),
  SKY_CLOUD_CUSHION(CardType.SKY, SkillBattleType.STABLE, "구름 쿠션", "폭신한 구름 쿠션으로 충격을 줄여요.", "cloud"),
  SKY_HALO_GLOW(CardType.SKY, SkillBattleType.BALANCED, "햇무리 빛", "둥근 햇무리 빛으로 주변을 밝혀요.", "sun"),
  SKY_UPDRAFT(CardType.SKY, SkillBattleType.GAMBLE, "상승 기류", "위로 솟는 바람을 타고 힘을 얻어요.", "wind"),
  SKY_AERIAL_POUNCE(CardType.SKY, SkillBattleType.GAMBLE, "공중 덮치기", "하늘에서 가볍게 뛰어내려 덮쳐요.", "paw"),
  SKY_MIST_WRAP(CardType.SKY, SkillBattleType.STABLE, "안개 감싸기", "얇은 안개로 몸을 감싸 보호해요.", "cloud"),
  SKY_SKYLINE_RUN(CardType.SKY, SkillBattleType.GAMBLE, "하늘선 질주", "하늘선을 따라 빠르게 달려나가요.", "wind"),
  SKY_TWINKLE_EYE(
      CardType.SKY, SkillBattleType.BALANCED, "반짝 눈빛", "반짝이는 눈빛으로 상대를 멈칫하게 해요.", "cloud"),
  SKY_BIRDSONG_CALL(
      CardType.SKY, SkillBattleType.BALANCED, "새소리 부름", "맑은 새소리로 친구 같은 기운을 불러요.", "wind"),
  SKY_LIGHT_RAINDROP(CardType.SKY, SkillBattleType.BALANCED, "빛방울", "작은 빛방울을 톡톡 떨어뜨려요.", "cloud"),
  SKY_COTTON_SHIELD(CardType.SKY, SkillBattleType.STABLE, "솜구름 방패", "솜구름처럼 부드러운 방패를 펼쳐요.", "cloud"),
  SKY_TAILWIND(CardType.SKY, SkillBattleType.BALANCED, "순풍 타기", "등 뒤의 순풍을 타고 속도를 높여요.", "wind"),
  SKY_CLEAR_MIND(CardType.SKY, SkillBattleType.STABLE, "맑은 마음", "맑은 하늘처럼 마음을 가다듬어요.", "cloud"),
  SKY_CLOUD_BURST(CardType.SKY, SkillBattleType.GAMBLE, "구름 폭발", "모인 구름 기운을 한 번에 터뜨려요.", "cloud"),

  SPACE_STAR_PUNCH(CardType.SPACE, SkillBattleType.GAMBLE, "별빛 펀치", "별빛을 담은 펀치로 반짝 공격해요.", "star"),
  SPACE_MOON_GUARD(CardType.SPACE, SkillBattleType.STABLE, "달빛 방어", "은은한 달빛 장막으로 몸을 지켜요.", "moon"),
  SPACE_COMET_DASH(CardType.SPACE, SkillBattleType.GAMBLE, "혜성 돌진", "혜성처럼 긴 빛을 남기며 돌진해요.", "star"),
  SPACE_NEBULA_HIDE(CardType.SPACE, SkillBattleType.STABLE, "성운 숨기", "보랏빛 성운 속에 몸을 숨겨요.", "star"),
  SPACE_ORBIT_STEP(
      CardType.SPACE, SkillBattleType.BALANCED, "궤도 걸음", "빙글 도는 궤도를 따라 움직여요.", "planet"),
  SPACE_GALAXY_SPARK(
      CardType.SPACE, SkillBattleType.BALANCED, "은하 반짝", "은하수처럼 반짝이는 기운을 뿌려요.", "star"),
  SPACE_METEOR_TAIL(CardType.SPACE, SkillBattleType.GAMBLE, "유성 꼬리", "꼬리 끝에서 유성빛을 흩뿌려요.", "star"),
  SPACE_COSMIC_PAW(CardType.SPACE, SkillBattleType.GAMBLE, "코스믹 발톱", "우주 기운을 담은 발톱으로 할퀴어요.", "paw"),
  SPACE_STARDUST_HEAL(
      CardType.SPACE, SkillBattleType.STABLE, "별가루 회복", "고운 별가루로 지친 몸을 달래요.", "star"),
  SPACE_BLACKHOLE_PULL(
      CardType.SPACE, SkillBattleType.GAMBLE, "블랙홀 당기기", "작은 중력장으로 상대를 끌어당겨요.", "planet"),
  SPACE_PLANET_BOUNCE(
      CardType.SPACE, SkillBattleType.BALANCED, "행성 튕기기", "동글동글 행성처럼 통통 튀어올라요.", "planet"),
  SPACE_LUNAR_NAP(CardType.SPACE, SkillBattleType.STABLE, "달잠", "고요한 달빛 아래 잠시 쉬어가요.", "moon"),
  SPACE_ASTRO_SHIELD(CardType.SPACE, SkillBattleType.STABLE, "우주 방패", "별빛 방패로 위험을 막아내요.", "star"),
  SPACE_ZERO_GRAVITY(
      CardType.SPACE, SkillBattleType.BALANCED, "무중력 점프", "무중력처럼 가볍게 뛰어올라요.", "planet"),
  SPACE_NOVA_FLASH(CardType.SPACE, SkillBattleType.GAMBLE, "신성 섬광", "강한 별빛을 번쩍 터뜨려요.", "star"),
  SPACE_SATELLITE_SCAN(
      CardType.SPACE, SkillBattleType.BALANCED, "위성 탐색", "작은 위성처럼 주변을 살펴요.", "planet"),
  SPACE_ALIEN_WINK(CardType.SPACE, SkillBattleType.BALANCED, "외계 윙크", "장난스러운 윙크로 흐름을 바꿔요.", "star"),
  SPACE_STAR_RING(CardType.SPACE, SkillBattleType.STABLE, "별고리", "작은 별고리를 만들어 몸을 감싸요.", "star"),
  SPACE_VOID_STEP(CardType.SPACE, SkillBattleType.STABLE, "공허 발걸음", "깊은 우주처럼 조용히 다가가요.", "moon"),
  SPACE_MILKY_WAY(CardType.SPACE, SkillBattleType.GAMBLE, "은하수 질주", "은하수 길을 따라 빠르게 달려요.", "star"),
  SPACE_ROCKET_JUMP(
      CardType.SPACE, SkillBattleType.GAMBLE, "로켓 점프", "로켓처럼 힘차게 뛰어오르며 돌파해요.", "planet"),
  SPACE_GRAVITY_HOLD(
      CardType.SPACE, SkillBattleType.STABLE, "중력 붙잡기", "작은 중력으로 중심을 단단히 잡아요.", "planet"),
  SPACE_COSMIC_DUST(
      CardType.SPACE, SkillBattleType.STABLE, "우주 먼지", "반짝이는 우주 먼지로 시야를 흐려요.", "star"),
  SPACE_ECLIPSE_POSE(
      CardType.SPACE, SkillBattleType.STABLE, "일식 자세", "빛을 가린 듯 차분한 자세를 취해요.", "moon"),
  SPACE_AURORA_WAVE(CardType.SPACE, SkillBattleType.BALANCED, "오로라 물결", "빛나는 오로라 물결을 펼쳐요.", "star"),
  SPACE_STAR_SEED(CardType.SPACE, SkillBattleType.BALANCED, "별씨앗", "작은 별씨앗을 심어 반짝임을 퍼뜨려요.", "star"),
  SPACE_MOONBEAM(CardType.SPACE, SkillBattleType.GAMBLE, "문빔", "달빛 한 줄기를 곧게 쏘아보내요.", "moon"),
  SPACE_PULSAR_BEAT(
      CardType.SPACE, SkillBattleType.BALANCED, "펄서 박동", "규칙적인 별의 박동으로 리듬을 잡아요.", "star"),
  SPACE_DIMENSION_SKIP(
      CardType.SPACE, SkillBattleType.GAMBLE, "차원 넘기", "짧은 순간 차원을 건너듯 피해서 움직여요.", "planet"),
  SPACE_UNIVERSE_CALL(
      CardType.SPACE, SkillBattleType.BALANCED, "우주의 부름", "끝없는 우주의 기운을 조용히 불러요.", "star"),

  SEA_WAVE_DASH(CardType.SEA, SkillBattleType.GAMBLE, "파도 돌진", "시원한 파도를 타고 힘차게 달려들어요.", "wave"),
  SEA_BUBBLE_GUARD(CardType.SEA, SkillBattleType.STABLE, "방울 방어", "동글동글 물방울로 몸을 보호해요.", "bubble"),
  SEA_CORAL_HIDE(CardType.SEA, SkillBattleType.STABLE, "산호 숨기", "알록달록 산호 사이에 몸을 숨겨요.", "coral"),
  SEA_SPLASH_PAW(CardType.SEA, SkillBattleType.GAMBLE, "물보라 발톱", "물보라를 튀기며 빠르게 할퀴어요.", "paw"),
  SEA_TIDE_PULL(CardType.SEA, SkillBattleType.GAMBLE, "조수 당기기", "밀려오는 조수처럼 상대를 끌어당겨요.", "wave"),
  SEA_PEARL_SHOT(CardType.SEA, SkillBattleType.GAMBLE, "진주 슛", "반짝이는 진주를 톡 쏘아보내요.", "bubble"),
  SEA_AQUA_HEAL(CardType.SEA, SkillBattleType.STABLE, "아쿠아 회복", "맑은 물의 기운으로 상처를 달래요.", "wave"),
  SEA_FOAM_ROLL(CardType.SEA, SkillBattleType.BALANCED, "거품 구르기", "하얀 거품 속을 데굴데굴 굴러요.", "bubble"),
  SEA_SEASHELL_SHIELD(CardType.SEA, SkillBattleType.STABLE, "조개 방패", "단단한 조개껍데기처럼 막아내요.", "coral"),
  SEA_BLUE_CURRENT(CardType.SEA, SkillBattleType.GAMBLE, "푸른 해류", "푸른 물살을 타고 빠르게 움직여요.", "wave"),
  SEA_WATER_TAIL(CardType.SEA, SkillBattleType.BALANCED, "물결 꼬리", "꼬리로 물결을 일으켜 흐름을 만들어요.", "wave"),
  SEA_REEF_JUMP(CardType.SEA, SkillBattleType.GAMBLE, "암초 점프", "암초를 박차고 통통 뛰어올라요.", "coral"),
  SEA_RAIN_DROP(CardType.SEA, SkillBattleType.BALANCED, "빗방울 톡톡", "작은 빗방울을 톡톡 떨어뜨려요.", "bubble"),
  SEA_MARINE_CALL(CardType.SEA, SkillBattleType.BALANCED, "바다의 부름", "넓은 바다의 힘을 조용히 불러요.", "wave"),
  SEA_DOLPHIN_STEP(CardType.SEA, SkillBattleType.BALANCED, "돌고래 스텝", "돌고래처럼 유연하게 방향을 바꿔요.", "wave"),
  SEA_WHIRLPOOL(CardType.SEA, SkillBattleType.GAMBLE, "소용돌이", "작은 소용돌이로 상대의 중심을 흔들어요.", "wave"),
  SEA_DRIFT_FLOAT(CardType.SEA, SkillBattleType.STABLE, "둥둥 표류", "물 위에 둥둥 떠서 힘을 아껴요.", "bubble"),
  SEA_ICE_SPLASH(CardType.SEA, SkillBattleType.BALANCED, "얼음 물보라", "차가운 물보라로 흐름을 늦춰요.", "wave"),
  SEA_KELP_WRAP(CardType.SEA, SkillBattleType.STABLE, "해초 감싸기", "부드러운 해초로 몸을 감싸요.", "coral"),
  SEA_SALT_SPARK(
      CardType.SEA, SkillBattleType.BALANCED, "소금 반짝", "반짝이는 소금 결정으로 시선을 끌어요.", "bubble"),
  SEA_OCEAN_NAP(CardType.SEA, SkillBattleType.STABLE, "바다 낮잠", "잔잔한 바다 위에서 잠시 쉬어가요.", "wave"),
  SEA_DEEP_BLUE(CardType.SEA, SkillBattleType.BALANCED, "딥블루", "깊은 바다빛 기운으로 침착해져요.", "wave"),
  SEA_TURTLE_GUARD(CardType.SEA, SkillBattleType.STABLE, "거북 방어", "거북 등껍질처럼 단단히 버텨요.", "coral"),
  SEA_STREAM_CUT(CardType.SEA, SkillBattleType.GAMBLE, "물살 베기", "날카로운 물살로 빠르게 베어내요.", "wave"),
  SEA_MIST_BREATH(CardType.SEA, SkillBattleType.STABLE, "물안개 숨결", "차분한 물안개 숨결을 내뿜어요.", "bubble"),
  SEA_RIPPLE_STEP(CardType.SEA, SkillBattleType.BALANCED, "잔물결 걸음", "잔물결처럼 조용하게 움직여요.", "wave"),
  SEA_LAGOON_REST(CardType.SEA, SkillBattleType.STABLE, "라군 휴식", "고요한 라군에서 기운을 회복해요.", "coral"),
  SEA_TIDAL_BOOST(CardType.SEA, SkillBattleType.BALANCED, "밀물 기운", "밀려오는 물결처럼 힘을 끌어올려요.", "wave"),
  SEA_SHELL_BITE(CardType.SEA, SkillBattleType.GAMBLE, "조개 깨물기", "작은 조개처럼 단단하게 물고 늘어져요.", "paw"),
  SEA_AQUA_FLASH(CardType.SEA, SkillBattleType.GAMBLE, "아쿠아 섬광", "푸른 물빛을 번쩍이며 파고들어요.", "wave");

  private final CardType cardType;
  private final SkillBattleType battleType;
  private final String displayName;
  private final String description;
  private final String iconKey;

  CardSkill(
      CardType cardType,
      SkillBattleType battleType,
      String displayName,
      String description,
      String iconKey) {
    this.cardType = cardType;
    this.battleType = battleType;
    this.displayName = displayName;
    this.description = description;
    this.iconKey = iconKey;
  }

  public static List<CardSkill> forType(CardType cardType) {
    return Arrays.stream(values()).filter(skill -> skill.cardType == cardType).toList();
  }

  public static List<CardSkill> forType(CardType cardType, SkillBattleType battleType) {
    return Arrays.stream(values())
        .filter(skill -> skill.cardType == cardType && skill.battleType == battleType)
        .toList();
  }

  public CardType cardType() {
    return cardType;
  }

  public SkillBattleType battleType() {
    return battleType;
  }

  public String displayName() {
    return displayName;
  }

  public String description() {
    return description;
  }

  public String iconKey() {
    return iconKey;
  }
}
