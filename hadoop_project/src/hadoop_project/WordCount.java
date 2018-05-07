package hadoop_project;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class WordCount {

    /*
     * �û��Զ���map����������<key, value>Ϊ����Ľ���ļ����д���
     * Map������Ҫ�̳�org.apache.hadoop.mapreduce����Mapper�࣬����д��map������
     * ͨ����map��������������keyֵ��valueֵ���������̨�Ĵ���
     * �����Է���map������valueֵ�洢�����ı��ļ��е�һ�У��Իس���Ϊ�н�����ǣ�����keyֵΪ���е�����ĸ������ı��ļ����׵�ַ��ƫ������
     * Ȼ��StringTokenizer�ཫÿһ�в�ֳ�Ϊһ�����ĵ���
     * ������<word,1>��Ϊmap�����Ľ�����������Ĺ���������MapReduce��ܴ��� ÿ�����ݵ���һ�� Tokenizer�����ʷִ���
     */
    public static class TokenizerMapper extends
            Mapper<Object, Text, Text, IntWritable> {
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();

        /*
         * ��дMapper���е�map����
         */
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            //System.out.println(value.toString());
            while (itr.hasMoreTokens()) {
                word.set(itr.nextToken());// ��ȡ�¸��ֶε�ֵ��д���ļ�
                context.write(word, one);
            }
        }
    }

    /*
     * �û��Զ���reduce����������ж���ȶȲ⣬��ÿ��reduce�����Լ���Ӧ��map�������
     * Reduce������Ҫ�̳�org.apache.hadoop.mapreduce����Reducer�࣬����д��reduce������
     * Map�������<key,values>��keyΪ�������ʣ���values�Ƕ�Ӧ���ʵļ���ֵ����ɵ��б�Map���������Reduce�����룬
     * ����reduce����ֻҪ����values����ͣ����ɵõ�ĳ�����ʵ��ܴ�����
     */
    public static class IntSumReducer extends
            Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();
        public void reduce(Text key, Iterable<IntWritable> values,
                Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {

        /**
         * ������������
         */
        File jarFile = EJob.createTempJar("bin");
        ClassLoader classLoader = EJob.getClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);

        /**
         * ����hadoop��Ⱥ����
         */
        Configuration conf = new Configuration(true);
        conf.set("fs.default.name", "hdfs://192.168.137.5:9000");
//        conf.set("hadoop.job.user", "root");
        conf.set("mapreduce.framework.name", "yarn");
//        conf.set("mapreduce.jobtracker.address", "192.168.137.5:9001");
        conf.set("yarn.resourcemanager.hostname", "192.168.137.5");
//        conf.set("yarn.resourcemanager.admin.address", "192.168.137.5:8033");
//        conf.set("yarn.resourcemanager.address", "192.168.137.5:8032");
//        conf.set("yarn.resourcemanager.resource-tracker.address", "192.168.137.5:8036");
//        conf.set("yarn.resourcemanager.scheduler.address", "192.168.137.5:8030");
        conf.set("mapreduce.app-submission.cross-platform", "true");

        String[] otherArgs = new String[2];
        otherArgs[0] = "hdfs://192.168.137.5:9000/test_in";//����ԭ�ļ�Ŀ¼������ǰ����������ļ�
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        otherArgs[1] = "hdfs://192.168.137.5:9000/test_out/" + time;//�����ļ������洢Ŀ¼��ÿ�γ���ִ�еĽ��Ŀ¼������ͬ���������ʱ���ǩ

        /*
         * setJobName()�����������Job����Job���к�������������ڸ�����ҵ�Job��
         * �Ա���JobTracker��Tasktracker��ҳ���ж�����м���
         */
        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(WordCount.class);

        ((JobConf) job.getConfiguration()).setJar(jarFile.toString());//�����������ã���Ӵ˾������eclipse��ֱ���ύmapreduce�����������java�ļ����jar������Ҫ���þ�ע�͵���������ִ��ʱ�����Ҳ�����������

        // job.setMaxMapAttempts(100);//���������ͼ������map�����������һ�������ø��������й����е�map����
        // job.setNumReduceTasks(5);//����reduce����������������ļ�������

        /*
         * Job�����Map����֣���Combiner���м����ϲ����Լ�Reduce���ϲ�������ش����ࡣ
         * ������Reduce��������Map�������м����ϲ���������������ݴ������ѹ����
         */
        job.setMapperClass(TokenizerMapper.class);// ִ���û��Զ���map����
        job.setCombinerClass(IntSumReducer.class);// ���û��Զ���map���������ݴ��������кϲ������Լ��ٴ�������
        job.setReducerClass(IntSumReducer.class);// ִ���û��Զ���reduce����

        /*
         * ��������Job������<key,value>����key��value�������ͣ���Ϊ�����<����,����>��
         * ����key����Ϊ"Text"���ͣ��൱��Java��String����
         * ��Value����Ϊ"IntWritable"���൱��Java�е�int���͡�
         */
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        /*
         * ���������ļ��л��ļ�·�������������ݵ�·��
         * ��������ļ����ݷָ��һ������split��������Щsplit�ֲ��<key,value>����Ϊ�����û��Զ���map����������
         * ���У�ÿ��split�ļ��Ĵ�С����С��hdfs���ļ����С
         * ��Ĭ��64M���������split�������������ȡ����hdfs���С��ʣ�ಿ�����ݣ������ͻ�������������ɼ����ٶ�Ӱ��
         * Ĭ��ʹ��TextInputFormat���ͣ�������������ʽΪ�ı����������ļ�
         */
        System.out.println("Job start!");
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));

        /*
         * ��������ļ�·�� Ĭ��ʹ��TextOutputFormat���ͣ������������ʽΪ�ı������ļ����ֶμ�Ĭ�����Ʊ������
         */
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));

        /*
         * ��ʼ������������ú��㷨
         */
        if (job.waitForCompletion(true)) {
            System.out.println("ok!");
        } else {
            System.out.println("error!");
            System.exit(0);
        }
    }
}
