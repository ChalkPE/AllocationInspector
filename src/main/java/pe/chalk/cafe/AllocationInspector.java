package pe.chalk.cafe;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import pe.chalk.takoyaki.Takoyaki;
import pe.chalk.takoyaki.Target;
import pe.chalk.takoyaki.model.Member;
import pe.chalk.takoyaki.utils.TextFormat;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2015-10-10
 */
public class AllocationInspector {
    public static final String MEMBER_RECENT_ARTICLES_URL = "http://cafe.naver.com/CafeMemberNetworkArticleList.nhn?clubid=%s&search.clubid=%s&search.writerid=%s&search.page=%d";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd.", Locale.KOREA);
    public static final SimpleDateFormat KOREAN_DATE_FORMAT = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
    public static final SimpleDateFormat KOREAN_FULL_DATE_FORMAT = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.KOREA);

    public static final String DELIMITER = TextFormat.RESET.toString() + TextFormat.DARK_BLUE + "| " + TextFormat.BLUE;

    private int clubId;
    private int allocatedArticles;
    private List<Member> assignees;

    public AllocationInspector(JSONObject properties){
        this.clubId = properties.getInt("clubId");
        this.allocatedArticles = properties.getInt("allocatedArticles");
        this.assignees = Takoyaki.<String>buildStream(properties.getJSONArray("assignees")).map(assignee -> {
            String[] a = assignee.split(":");
            return new Member(this.getTarget().getClubId(), a[0], a[1]);
        }).collect(Collectors.toList());
    }

    public int getClubId(){
        return clubId;
    }

    public int getAllocatedArticles(){
        return this.allocatedArticles;
    }

    public List<Member> getAssignees(){
        return this.assignees;
    }

    public Target getTarget(){
        return Takoyaki.getInstance().getTarget(this.getClubId());
    }

    public List<MemberArticle> getRecentMemberArticles(Member member, int page) throws IOException {
        Main.delay(1000);
        Takoyaki.getInstance().getLogger().debug(String.format("PARSE:  #%02d - %s", page, member));

        URL url = new URL(String.format(AllocationInspector.MEMBER_RECENT_ARTICLES_URL, this.getClubId(), this.getClubId(), member.getId(), page));
        return Jsoup.parse(Main.staff.getPage(url).getWebResponse().getContentAsString())
                .select("tr[align=center]:not([class])").stream()
                .map(element -> MemberArticle.fromElement(element, this.getClubId(), member))
                .sorted((a, b) -> a.getId() - b.getId())
                .collect(Collectors.toList());
    }

    public List<MemberArticle> getArticles(Member member, Date date){
        return this.getArticles(member, date, 1);
    }

    public List<MemberArticle> getArticles(Member member, Date date, int page){
        final String today = AllocationInspector.DATE_FORMAT.format(date);
        try{
            List<MemberArticle> articles = this.getRecentMemberArticles(member, page);
            if(articles.get(0).getUploadDate().compareTo(today) >= 0 || articles.stream().map(MemberArticle::getUploadDate).distinct().count() == 1){
                articles.addAll(this.getArticles(member, date, page + 1));
            }

            return articles.stream().filter(article -> article.getUploadDate().equals(today))
                    .filter(article -> article.getMenuId() != 30)
                    .collect(Collectors.toList());
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public String inspect(Date date){
        List<String> messages = new ArrayList<>();
        messages.add(String.format("%s%s[%s]   %s%s검사시각: %s%n",
                TextFormat.BOLD, TextFormat.BLUE, AllocationInspector.KOREAN_DATE_FORMAT.format(date),
                TextFormat.RESET, TextFormat.GRAY, AllocationInspector.KOREAN_FULL_DATE_FORMAT.format(new Date())));

        final long start = System.currentTimeMillis();

        final List<Result> results = this.getAssignees().stream().map(assignee -> new Result(assignee, this.getArticles(assignee, date), this.getAllocatedArticles())).sorted().collect(Collectors.toList());
        messages.addAll(this.printResultsWithRank(results));

        final long aliveAssignees     = results.stream().filter(Result::isAlive).count();
        final long succeededAssignees = results.stream().filter(Result::isSucceeded).count();
        final long totalArticles      = results.stream().mapToLong(Result::size).sum();

        final int totalAssignees = this.getAssignees().size();

        final double average             =      totalArticles * 1.0 / totalAssignees;
        final double alivePercentage     =     aliveAssignees * 1.0 / totalAssignees;
        final double succeededPercentage = succeededAssignees * 1.0 / totalAssignees;

        final Map<Integer, Long> map = results.stream().collect(Collectors.groupingBy(Result::size, Collectors.counting()));
        final double standardDeviation = Math.sqrt(map.entrySet().stream().mapToDouble(entry -> Math.pow(entry.getKey() - average, 2) * entry.getValue()).sum() / totalAssignees);

        messages.add(String.format("%s참여자: %s%4d명 %s달성자: %s%4d명 %s총합: %s%5d개 %s소요시간: %s%6.2f초 %s",
                AllocationInspector.DELIMITER, TextFormat.BOLD, aliveAssignees,
                AllocationInspector.DELIMITER, TextFormat.BOLD, succeededAssignees,
                AllocationInspector.DELIMITER, TextFormat.BOLD, totalArticles,
                AllocationInspector.DELIMITER, TextFormat.BOLD, (System.currentTimeMillis() - start) / 1000.0,
                AllocationInspector.DELIMITER));

        messages.add(String.format("%s참여율:　%s%4.1f%% %s달성률:　%s%4.1f%% %s평균: %s%5.2f개 %s표준편차: %s%6.2f개 %s%n%n",
                AllocationInspector.DELIMITER, TextFormat.BOLD, alivePercentage * 100,
                AllocationInspector.DELIMITER, TextFormat.BOLD, succeededPercentage * 100,
                AllocationInspector.DELIMITER, TextFormat.BOLD, average,
                AllocationInspector.DELIMITER, TextFormat.BOLD, standardDeviation,
                AllocationInspector.DELIMITER));

        return String.join(String.format("%n"), messages);
    }

    private List<String> printResultsWithRank(final List<Result> results){
        final List<Rank<Result>> ranks = new ArrayList<>();
        final int size = results.size();

        Rank<Result> currentRank = new Rank<>(0);
        for(int i = 0; i < size; i++){
            Result result = results.get(i);

            if(i != 0 && Result.COMPARATOR.compare(results.get(i - 1), result) != 0){
                ranks.add(currentRank);
                currentRank = new Rank<>(i);
            }
            currentRank.add(result);
        }
        ranks.add(currentRank);

        return ranks.stream().flatMap((final Rank<Result> rank) -> rank.getRankers(Comparator.comparing((Result result) -> result.getWriter().toString())).stream().map(result -> result.toString(String.format("#%02d ", rank.getRank() + 1)))).collect(Collectors.toList());
    }

    class Rank<T>{
        private int rank;
        private ArrayList<T> rankers;

        public Rank(int rank){
            this(rank, new ArrayList<>());
        }

        public Rank(int rank, ArrayList<T> rankers){
            this.rank = rank;
            this.rankers = rankers;
        }

        public int getRank(){
            return this.rank;
        }

        public ArrayList<T> getRankers(){
            return this.rankers;
        }

        public ArrayList<T> getRankers(Comparator<T> comparator){
            ArrayList<T> rankers = new ArrayList<>(this.getRankers());
            Collections.sort(rankers, comparator);

            return rankers;
        }

        public boolean add(T t){
            return rankers.add(t);
        }
    }
}
