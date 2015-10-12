package pe.chalk.cafe;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
    public static final String MEMBER_RECENT_ARTICLES_URL = "http://cafe.naver.com/CafeMemberNetworkArticleList.nhn?clubid=%s&search.clubid=%s&search.writerid=%s";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd.", Locale.KOREA);
    public static final SimpleDateFormat KOREAN_DATE_FORMAT = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);

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

    public Document getRecentMemberArticles(Member member) throws IOException {
        Main.delay(50);
        return Jsoup.parse(Main.staff.getPage(new URL(String.format(AllocationInspector.MEMBER_RECENT_ARTICLES_URL, this.getClubId(), this.getClubId(), member.getId()))).getWebResponse().getContentAsString());
    }

    public List<MemberArticle> getArticles(Member member, Date date){
        final String today = AllocationInspector.DATE_FORMAT.format(date);
        try{
            return this.getRecentMemberArticles(member)
                    .select("tr[align=center]:not([class])").stream()
                    .map(element -> MemberArticle.fromElement(element, this.getClubId(), member))
                    .filter(article -> article.getUploadDate().equals(today))
                    .filter(article -> article.getMenuId() != 30)
                    .sorted((a, b) -> a.getId() - b.getId())
                    .collect(Collectors.toList());
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public void inspect(Date date){
        Takoyaki.getInstance().getLogger().info(TextFormat.BOLD.toString() + TextFormat.BLUE + "[" + AllocationInspector.KOREAN_DATE_FORMAT.format(date) + "]");
        final long start = System.currentTimeMillis();

        final List<Result> results = this.getAssignees().stream().map(assignee -> new Result(assignee, this.getArticles(assignee, date), this.getAllocatedArticles())).sorted().collect(Collectors.toList());
        this.printResultsWithRank(results);

        final long aliveAssignees     = results.stream().filter(Result::isAlive).count();
        final long succeededAssignees = results.stream().filter(Result::isSucceeded).count();
        final long totalArticles      = results.stream().mapToLong(Result::size).sum();

        final int totalAssignees = this.getAssignees().size();

        final double average             =      totalArticles * 1.0 / totalAssignees;
        final double alivePercentage     =     aliveAssignees * 1.0 / totalAssignees;
        final double succeededPercentage = succeededAssignees * 1.0 / totalAssignees;

        final Map<Integer, Long> map = results.stream().collect(Collectors.groupingBy(Result::size, Collectors.counting()));
        final double standardDeviation = Math.sqrt(map.entrySet().stream().mapToDouble(entry -> Math.pow(entry.getKey() - average, 2) * entry.getValue()).sum() / totalAssignees);

        Takoyaki.getInstance().getLogger().info(String.format("%s참여자: %s%4d명 %s달성자: %s%4d명 %s총합: %s%5d개 %s소요시간: %s%5.2f초 %s",
                AllocationInspector.DELIMITER, TextFormat.BOLD, aliveAssignees,
                AllocationInspector.DELIMITER, TextFormat.BOLD, succeededAssignees,
                AllocationInspector.DELIMITER, TextFormat.BOLD, totalArticles,
                AllocationInspector.DELIMITER, TextFormat.BOLD, (System.currentTimeMillis() - start) / 1000.0,
                AllocationInspector.DELIMITER));

        Takoyaki.getInstance().getLogger().info(String.format("%s참여율:　%s%4.1f%% %s달성률:　%s%4.1f%% %s평균: %s%5.2f개 %s표준편차: %s%5.2f개 %s%n",
                AllocationInspector.DELIMITER, TextFormat.BOLD, alivePercentage * 100,
                AllocationInspector.DELIMITER, TextFormat.BOLD, succeededPercentage * 100,
                AllocationInspector.DELIMITER, TextFormat.BOLD, average,
                AllocationInspector.DELIMITER, TextFormat.BOLD, standardDeviation,
                AllocationInspector.DELIMITER));
    }

    private void printResultsWithRank(final List<Result> results){
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

        ranks.forEach(rank -> rank.getRankers(Comparator.comparing((Result result) -> result.getWriter().toString())).forEach(result -> Takoyaki.getInstance().getLogger().info(result.toString(String.format("#%02d", rank.getRank() + 1)))));
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
