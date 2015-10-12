package pe.chalk.cafe;

import pe.chalk.takoyaki.model.Member;
import pe.chalk.takoyaki.utils.TextFormat;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2015-10-12
 */
public class Result implements Comparable<Result>, Comparator<Result> {
    public static final Comparator<Result> COMPARATOR = Comparator.comparingInt((Result result) -> -result.getArticles().size()).thenComparingInt(result -> result.getMajorArticleId().orElse(Integer.MAX_VALUE));

    private Member writer;
    private List<MemberArticle> articles;
    private int allocatedArticles;

    public Result(Member writer, List<MemberArticle> articles, int allocatedArticles){
        this.writer = writer;
        this.articles = articles;
        this.allocatedArticles = allocatedArticles;
    }

    public Member getWriter(){
        return this.writer;
    }

    public List<MemberArticle> getArticles(){
        return this.articles;
    }

    public int getAllocatedArticles(){
        return this.allocatedArticles;
    }

    public MemberArticle get(int index){
        return this.getArticles().get(index);
    }

    public int size(){
        return this.getArticles().size();
    }

    public boolean isEmpty(){
        return this.getArticles().isEmpty();
    }

    public boolean isAlive(){
        return !this.isEmpty();
    }

    public boolean isHalfDone(){
        return this.size() >= this.getAllocatedArticles() / 2.0;
    }

    public boolean isSucceeded(){
        return this.size() >= this.getAllocatedArticles();
    }

    public boolean isExceed(){
        return this.size() > this.getAllocatedArticles();
    }

    public Optional<MemberArticle> getMajorArticle(){
        return Optional.ofNullable(this.isEmpty() ? null : this.get(this.isSucceeded() ? this.getAllocatedArticles() - 1 : 0));
    }

    public Optional<Integer> getMajorArticleId(){
        Optional<MemberArticle> majorArticle = this.getMajorArticle();
        return Optional.ofNullable(majorArticle.isPresent() ? majorArticle.get().getId() : null);
    }

    public Optional<String> getMajorArticleUploadDateAndTime(){
        Optional<MemberArticle> majorArticle = this.getMajorArticle();
        return Optional.ofNullable(majorArticle.isPresent() ? majorArticle.get().getUploadDateAndTime().substring(12) : null);
    }

    public TextFormat getColor(){
        if(this.isEmpty()) return TextFormat.DARK_RED;
        else if(this.isExceed()) return TextFormat.AQUA;
        else if(this.isSucceeded()) return TextFormat.GREEN;
        else if(this.isHalfDone()) return TextFormat.YELLOW;
        else return TextFormat.RED;
    }

    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") Result that){
        return this.compare(this, that);
    }

    @Override
    public int compare(Result a, Result b){
        return Result.COMPARATOR.compare(a, b);
    }

    @Override
    public String toString(){
        return this.toString("");
    }

    public String toString(String prefix){
        return String.format("%s%s%s%2d/%-2d %s %s%s%s %s",
                TextFormat.BOLD, this.getColor(), prefix, this.size(), this.getAllocatedArticles(), this.isSucceeded() ? "SUCCESS" : "FAILURE",
                TextFormat.RESET, this.getColor(), this.getMajorArticleUploadDateAndTime().orElse("     "), this.getWriter());
    }
}
