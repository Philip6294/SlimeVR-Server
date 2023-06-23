import { useLocalization } from '@fluent/react';
import { useState } from 'react';
import { useOnboarding } from '../../../hooks/onboarding';
import { Button } from '../../commons/Button';
import { SlimeVRIcon } from '../../commons/icon/SimevrIcon';
import { LangSelector } from '../../commons/LangSelector';
import { Typography } from '../../commons/Typography';
import { SkipSetupButton } from '../SkipSetupButton';
import { SkipSetupWarningModal } from '../SkipSetupWarningModal';

export function HomePage() {
  const { l10n } = useLocalization();
  const { applyProgress, skipSetup } = useOnboarding();
  const [skipWarning, setSkipWarning] = useState(false);

  applyProgress(0.1);

  return (
    <>
      <div className="flex flex-col gap-5 h-full items-center w-full justify-center relative px-4">
        <SkipSetupButton
          visible={true}
          modalVisible={skipWarning}
          onClick={() => setSkipWarning(true)}
        ></SkipSetupButton>
        <div className="flex flex-col gap-5 items-center z-10 scale-150 mb-20">
          <SlimeVRIcon></SlimeVRIcon>
          <Typography variant="mobile-title">
            {l10n.getString('onboarding-home')}
          </Typography>
          <Button variant="primary" to="/onboarding/wifi-creds">
            {l10n.getString('onboarding-home-start')}
          </Button>
        </div>
        <div className="absolute right-4 bottom-4 z-50">
          <LangSelector />
        </div>
        <div
          className="absolute bg-accent-background-50 w-full rounded-full"
          style={{
            bottom: 'calc(-300vw / 1.04)',
            height: '300vw',
            width: '300vw',
          }}
        ></div>
        <img
          className="absolute"
          src="/images/slime-girl.png"
          style={{
            width: '35%',
            maxWidth: 800,
            bottom: '1%',
            left: '9%',
          }}
        />
        <img
          className="absolute"
          src="/images/slimes.png"
          style={{
            width: '35%',
            maxWidth: 800,
            bottom: '1%',
            right: '9%',
          }}
        />
      </div>
      <SkipSetupWarningModal
        accept={skipSetup}
        onClose={() => setSkipWarning(false)}
        isOpen={skipWarning}
      ></SkipSetupWarningModal>
    </>
  );
}
