/*
 * Copyright 2015-2016 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package embl.ebi.variation.eva.pipeline.jobs;

import embl.ebi.variation.eva.pipeline.steps.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.TaskletStepBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
@EnableBatchProcessing
@Import(VariantJobArgsConfig.class)
public class VariantConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VariantConfiguration.class);
    public static final String jobName = "variantJob";

    @Autowired
    JobBuilderFactory jobBuilderFactory;
    @Autowired
    StepBuilderFactory stepBuilderFactory;
    @Autowired
    JobLauncher jobLauncher;
    @Autowired
    Environment environment;

    @Bean
    public Job variantJob() {
        JobBuilder jobBuilder = jobBuilderFactory
                .get(jobName)
                .incrementer(new RunIdIncrementer());

        return jobBuilder
                .start(transform())
                .next(load())
                .next(statsCreate())
                .next(statsLoad())
                .next(annotationGenerateInput())
                .next(annotationCreate())
                .next(annotationLoad())
                .build();
    }

    @Bean
    public VariantsTransform variantsTransform(){
         return new VariantsTransform();
    }

    public Step transform() {
        StepBuilder step1 = stepBuilderFactory.get("transform");
        TaskletStepBuilder tasklet = step1.tasklet(variantsTransform());

        // true: every job execution will do this step, even if this step is already COMPLETED
        // false: if the job was aborted and is relaunched, this step will NOT be done again
        tasklet.allowStartIfComplete(true);

        return tasklet.build();
    }

    @Bean
    public VariantsLoad variantsLoad(){
        return new VariantsLoad();
    }

    public Step load() {
        StepBuilder step1 = stepBuilderFactory.get("load");
        TaskletStepBuilder tasklet = step1.tasklet(variantsLoad());

        // true: every job execution will do this step, even if this step is already COMPLETED
        // false: if the job was aborted and is relaunched, this step will NOT be done again
        tasklet.allowStartIfComplete(true);
        return tasklet.build();
    }

    @Bean
    public VariantsStatsCreate variantsStatsCreate(){
        return new VariantsStatsCreate();
    }

    public Step statsCreate() {
        StepBuilder step1 = stepBuilderFactory.get("statsCreate");
        TaskletStepBuilder tasklet = step1.tasklet(variantsStatsCreate());

        // true: every job execution will do this step, even if this step is already COMPLETED
        // false: if the job was aborted and is relaunched, this step will NOT be done again
        tasklet.allowStartIfComplete(true);
        return tasklet.build();
    }

    @Bean
    public VariantsStatsLoad variantsStatsLoad(){
        return new VariantsStatsLoad();
    }

    public Step statsLoad() {
        StepBuilder step1 = stepBuilderFactory.get("statsLoad");
        TaskletStepBuilder tasklet = step1.tasklet(variantsStatsLoad());

        // true: every job execution will do this step, even if this step is already COMPLETED
        // false: if the job was aborted and is relaunched, this step will NOT be done again
        tasklet.allowStartIfComplete(true);
        return tasklet.build();
    }

    @Bean
    public VariantsAnnotGenerateInput variantsAnnotGenerateInput(){
        return new VariantsAnnotGenerateInput();
    }

    public Step annotationGenerateInput() {
        StepBuilder step1 = stepBuilderFactory.get("annotationGenerateInput");
        TaskletStepBuilder tasklet = step1.tasklet(variantsAnnotGenerateInput());

        // true: every job execution will do this step, even if this step is already COMPLETED
        // false: if the job was aborted and is relaunched, this step will NOT be done again
        tasklet.allowStartIfComplete(true);
        return tasklet.build();
    }

    @Bean
    public VariantsAnnotCreate variantsAnnotCreate(){
        return new VariantsAnnotCreate();
    }

    public Step annotationCreate() {
        StepBuilder step1 = stepBuilderFactory.get("annotationCreate");
        TaskletStepBuilder tasklet = step1.tasklet(variantsAnnotCreate());

        // true: every job execution will do this step, even if this step is already COMPLETED
        // false: if the job was aborted and is relaunched, this step will NOT be done again
        tasklet.allowStartIfComplete(true);
        return tasklet.build();
    }

    @Bean
    public VariantsAnnotLoad variantsAnnotLoad(){
        return new VariantsAnnotLoad();
    }

    public Step annotationLoad() {
        StepBuilder step1 = stepBuilderFactory.get("annotationLoad");
        TaskletStepBuilder tasklet = step1.tasklet(variantsAnnotLoad());

        // true: every job execution will do this step, even if this step is already COMPLETED
        // false: if the job was aborted and is relaunched, this step will NOT be done again
        tasklet.allowStartIfComplete(true);
        return tasklet.build();
    }

}
