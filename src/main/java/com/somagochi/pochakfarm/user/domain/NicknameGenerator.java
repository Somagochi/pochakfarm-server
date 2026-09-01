package com.somagochi.pochakfarm.user.domain;

import com.somagochi.pochakfarm.common.random.RandomProvider;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NicknameGenerator {

  private static final List<String> ADJECTIVES =
      List.of(
          "행복", "용감", "든든", "상냥", "씩씩", "튼튼", "포근", "반짝", "발랄", "명랑", "유쾌", "상큼", "달콤", "새콤", "몽실",
          "방실", "소복", "촉촉", "폭신", "말랑", "쫀득", "보들", "사뿐", "살랑", "두근", "초롱", "총총", "도톰", "통통", "동글",
          "짱짱", "탱탱", "싱싱", "화사", "산뜻", "깔끔", "청량", "시원", "따뜻", "훈훈", "깜찍", "앙증", "귀염", "늠름", "당당",
          "우아", "고요", "잔잔", "활발", "열정", "뽀송", "포실", "나긋", "해맑", "빛나", "슬기", "정직", "성실", "근면", "온화",
          "온순", "순박", "소박", "담백", "고소", "살뜰", "알뜰", "아늑", "흐뭇", "뿌듯", "다정", "맑은", "밝은", "붉은", "푸른",
          "하얀", "노란", "금빛", "은빛", "알찬", "야무", "당돌", "대담", "침착", "차분", "느긋", "넉넉", "푸근", "다부", "옹골",
          "굳센", "힘찬", "날쌘", "잽싼", "날렵", "민첩", "총명", "영리", "기특", "어진");

  private static final List<String> NOUNS =
      List.of(
          "토끼", "여우", "사슴", "오리", "거위", "하마", "낙타", "표범", "수달", "담비", "고래", "상어", "문어", "참새", "까치",
          "제비", "기린", "늑대", "사자", "판다", "펭귄", "나비", "개미", "매미", "나무", "감자", "당근", "배추", "호박", "딸기",
          "포도", "사과", "수박", "참외", "자두", "대추", "보리", "상추", "오이", "가지", "마늘", "양파", "생강", "버섯", "완두",
          "새싹", "씨앗", "열매", "구름", "햇살", "노루", "물개", "물소", "들소", "산양", "염소", "젖소", "황소", "잉어", "붕어",
          "송어", "연어", "광어", "갈치", "멸치", "새우", "조개", "소라", "전복", "낙지", "참치", "가재", "꿀벌", "여치", "황새",
          "백조", "공작", "원앙", "호두", "밤톨", "더덕", "우엉", "연근", "토란", "부추", "쑥갓", "냉이", "달래", "참깨", "들깨",
          "녹두", "살구", "자몽", "망고", "레몬", "체리", "멜론", "앵두", "이슬", "노을");

  private static final int SUFFIX_BOUND = 100;

  private final RandomProvider randomProvider;

  public NicknameGenerator(RandomProvider randomProvider) {
    this.randomProvider = randomProvider;
  }

  public String generate() {
    String adjective = ADJECTIVES.get(randomProvider.nextInt(ADJECTIVES.size()));
    String noun = NOUNS.get(randomProvider.nextInt(NOUNS.size()));
    return "%s%s%02d".formatted(adjective, noun, randomProvider.nextInt(SUFFIX_BOUND));
  }
}
