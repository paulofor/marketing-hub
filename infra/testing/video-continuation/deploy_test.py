"""Valida em memória o plano operacional antes de qualquer intervenção autorizada."""
import importlib.util
import unittest

spec=importlib.util.spec_from_file_location('apply','infra/testing/video-continuation/apply-image.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


class PlanTest(unittest.TestCase):
    def test_five_targets_preserve_configuration_and_dependencies(self):
        for name,(service,repository) in m.TARGETS.items():
            with self.subTest(service=service):
                current={'Name':'/'+name,'Config':{'Env':['SECRET=synthetic$a','PLAY=true'],'Labels':{'com.docker.compose.service':service,'com.docker.compose.project.config_files':'/fixture/compose.yml'}}}
                config={'services':{service:{'environment':{'SECRET':'wrong'},'image':'old','build':'.','depends_on':['unrelated'],'volumes':['/fixture:/data:ro'],'networks':{'default':{'aliases':['canonical']}}},'unrelated':{'image':'untouched'}},'networks':{'default':{'external':True}}}
                result=m.plan(config,current,f'marketing-hub/{repository}:vega-approved-{"a"*12}','a'*64)
                self.assertEqual(list(result['services']),[service])
                self.assertEqual(result['services'][service]['environment'],{'SECRET':'synthetic$a','PLAY':'true'})
                for key in ['volumes','networks']:self.assertEqual(result['services'][service][key],config['services'][service][key])
                self.assertNotIn('build',result['services'][service]);self.assertNotIn('depends_on',result['services'][service])
                self.assertEqual(config['services'][service]['image'],'old')
                self.assertEqual(m.literal(result)['services'][service]['environment']['SECRET'],'synthetic$$a')

    def test_wrong_target_and_revision_are_rejected(self):
        for name,digest in [('unrelated','a'*64),('marketinghub-backend','not-a-revision')]:
            with self.assertRaises(ValueError):m.plan({}, {'Name':'/'+name},'wrong',digest)


if __name__=='__main__':unittest.main()
